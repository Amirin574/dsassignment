package com.hep88.view
import akka.actor.typed.ActorRef
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.control.{Alert, Button, Label, ListView, TextField}
import com.hep88.ChatClient
import com.hep88.User
import com.hep88.Client
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scala.collection.mutable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
@sfxml
class MainWindowController( private val txtName: TextField,
                           private val lblStatus: Label, private val listUser: ListView[User],
                            private val joinButton : Button, private val startGroup : Button

                          ) {

  var chatClientRef: Option[ActorRef[ChatClient.Command]] = None

  var groupChatListView : ListView[User] = new ListView[User]()
  var groupChatMessageListView : ListView[String] = new ListView[String]()

  var privateChatListView : ListView[User] = new ListView[User]()
  var privateChatMessageListView : ListView[String] = new ListView[String]()

  val groupChatMessageMap : mutable.Map[String, ObservableBuffer[String]] = mutable.HashMap.empty
  val privateChatMessageMap : mutable.Map[String, ObservableBuffer[String]] = mutable.HashMap.empty



  def handleJoin(action: ActionEvent): Unit = {
    if(txtName != null) {
      chatClientRef map (_ ! ChatClient.StartJoin(txtName.text()))
      joinButton.disable = true
    }
  }


  def displayStatus(text: String): Unit = {
    lblStatus.text = text
  }
  def updateList(x: Iterable[User]): Unit ={
    listUser.items = new ObservableBuffer[User]() ++= x

  }

  // function to create create group chat window
  //in the creategroupchatwindowcontroller, theres a button to send request to the chatclient
  // to run this function and create a window for user

  def startGroupChat(): Unit = {
    try {
      val loader = new FXMLLoader(getClass.getResource("CreateGroupChatWindow.fxml"), NoDependencyResolver)
      val root: javafx.scene.Parent = loader.load()
      val control = loader.getController[com.hep88.view.CreateGroupChatWindowController#Controller]()

      control.setUserList(listUser, chatClientRef)

      val stage = new Stage() {
        title = "Create Group Chat"
        scene = new Scene(root)

      }
      control.setStage(stage)
      stage.show()

    } catch {
      case e: Exception => e.printStackTrace()
    }
  }



//function to create group chat window to send requests to chatclient
//in the groupchatwindowcontroller, theres a button to send request to the chatclient to run this function
// and create a window for each user in the group chat user list
  def openGroupChat(user : List[User]): Unit = {
    // tell the user they have been added to a group chat
    try {
      val alert = new Alert(AlertType.Information) {
        title = "Welcome"
        headerText = "Added to group chat"
        contentText = "You have been added to a group chat"
      }
      alert.showAndWait()

      //load the stage,fxml and controller
      val loader = new FXMLLoader(getClass.getResource("GroupChatWindow.fxml"), NoDependencyResolver)
      val root: javafx.scene.Parent = loader.load()
      val control = loader.getController[com.hep88.view.GroupChatWindowController#Controller]()
      //to update the group chat list when each window is initiated for each member in group chat


      val list : List[User] = user

      var key : String = ""
      list.foreach{
        user => key+= user.name
      }
      control.setName(txtName.getText())
      groupChatMessageMap(key) = new ObservableBuffer[String]()

      groupChatListView.items = ObservableBuffer(list)
      control.setGroupChatList(groupChatListView)

      //updates the group chat messages when user sends a message to all user
      groupChatMessageListView.items = groupChatMessageMap(key)
      control.setGroupChatMessage(groupChatMessageListView)

      val stage = new Stage() {
        title = "Group Chat"
        scene = new Scene(root)
      }

      stage.show()

    } catch {
      case e: Exception => e.printStackTrace()
    }
  }


  //the groupchatwindowcontroller sends a request to the chatclient and the chatclient will
  // route each user in the group chat to update the group chat messages
  def updateGroupChatMessages( msg : String, key : String) : Unit = {
      groupChatMessageMap(key) += msg

  }

  def updatePrivateChatMessages( msg : String, key : String) : Unit = {
    privateChatMessageMap(key) += msg

  }



  // this handles when a user wants to start a personal chat with another user we will send a request to chatclient and the chatclient
  //will route the user involved to open a new window to show their private chat
  def startPrivateChat() : Unit = {
    if(listUser.selectionModel().selectedItem.value.ref  != chatClientRef.get) {
      if (listUser.selectionModel().selectedIndex.value >= 0) {
        var privateChatList : List[User] = List()
        listUser.getItems.foreach {
          user =>
            if (user.name == txtName.getText() || user.name == listUser.selectionModel().selectedItem.value.name) {
              privateChatList = privateChatList :+ user
            }
        }
        Client.greeterMain ! ChatClient.StartPrivateChat(privateChatList)
      }
    } else {

      val alert = new Alert(AlertType.Information) {
        title = "Error Private Chat"
        headerText = "Could not start private chat"
        contentText = "You are not allowed to start private chat on yourself"
      }
      alert.showAndWait()

    }


  }

//opens a window for a private chat session
  def openPrivateChat(user : List[User]) : Unit = {
    try {

      val alert = new Alert(AlertType.Information) {
        title = "Welcome"
        headerText = "Private Chat"
        contentText = "You have been added to a private chat"
      }
      alert.showAndWait()

      val loader = new FXMLLoader(getClass.getResource("PrivateChatWindow.fxml"), NoDependencyResolver)
      val root: javafx.scene.Parent = loader.load()
      val control = loader.getController[com.hep88.view.PrivateChatWindowController#Controller]()
      val list : List[User] = user

      var key : String = ""
      list.foreach{
        user => key+= user.name
      }
      control.setName(txtName.getText())
      privateChatMessageMap(key) = new ObservableBuffer[String]()

      privateChatListView.items = ObservableBuffer(list)
      control.setPrivateChatList(privateChatListView)


      privateChatMessageListView.items = privateChatMessageMap(key)
      control.setPrivateChatMessage(privateChatMessageListView)



      val stage = new Stage() {
        title = "Private Chat"
        scene = new Scene(root)

      }

      stage.show()

    } catch {
      case e: Exception => e.printStackTrace()
    }
  }





}
