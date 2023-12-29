package com.hep88.view
import com.hep88.User
import scalafx.scene.control.{Button, ListView}
import scalafxml.core.macros.sfxml
import akka.actor.typed.ActorRef
import com.hep88.ChatClient
import com.hep88.User
import com.hep88.Client
import scalafx.Includes._
import scalafx.collections.{ObservableBuffer, ObservableHashSet}
import scalafx.scene.Scene
import scalafx.stage.Stage
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType


@sfxml
class CreateGroupChatWindowController(
                                       private var userList: ListView[User] , private val addedUser: ListView[User],
                                       private val addUserButton : Button, private val createGroupButton : Button
                                     ) {



  val groupChatUsers: ObservableBuffer[User] = ObservableBuffer[User]()
  val userToAdd: ObservableBuffer[User] = ObservableBuffer[User]()
  var createGroupChatStage: Stage = new Stage()

  //get user list from mainwindow user list, automatically add the user who created group chat to add user list
  def setUserList(list: ListView[User], ref : Option[ActorRef[ChatClient.Command]]): Unit = {

    list.getItems.foreach {
      user =>
        if(user.ref == ref.get) {
          userToAdd.add(user)
        }
        else {
          groupChatUsers.add(user)
        }

    }
    addedUser.items = userToAdd
    userList.items = groupChatUsers
  }
 //return added user list
  def getUserList() : ObservableBuffer[User]  = {
    userToAdd
  }

  //set the stage in order to close it when user press on create group button
  def setStage(stage : Stage) : Unit = {
    createGroupChatStage = stage
  }

  //add a user to the addeduser list
  def addUser(): Unit = {
    if (userList.selectionModel().selectedIndex.value >= 0) {
      val selectedUser = userList.selectionModel().selectedItem.value
      userToAdd.add(selectedUser)
      groupChatUsers.remove(selectedUser)
      addedUser.items = userToAdd
    }

  }

  //button to initiate the group chat, sends a request to the chatclient so each user in addeduser list will open a new window for group chat
  def initializeGroupChat(): Unit = {

    if(addedUser.getItems.size >= 3) {
      createGroupChatStage.close()

      userToAdd.foreach {
        users => users.ref ! ChatClient.OpenWindow(userToAdd.toList)
      }

    }
    else {
      val alert = new Alert(AlertType.Information) {
        initOwner(createGroupChatStage)
        title = "Alert"
        headerText = "Error when creating group chat"
        contentText = "The group chat must contain at least 3 person"
      }
      alert.showAndWait()
    }

  }

}

