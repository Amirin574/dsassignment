package com.hep88.view
import com.hep88.User
import scalafx.scene.control.{Button, ListView, TextField}
import scalafxml.core.macros.sfxml
import com.hep88.ChatClient
import scalafx.Includes._
import com.hep88.Client
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scalafx.collections.{ObservableBuffer, ObservableHashSet}

@sfxml
class GroupChatWindowController(private val groupChatMessages : ListView[String], private val groupChatList : ListView[User],
                                private val textMessage : TextField) {




  var userName : String = ""

  // set the group chat list of users to the list view
  def setGroupChatList(users : ListView[User]) : Unit = {
    groupChatList.items = users.getItems

  }
//set the group chat messages to the list view
  //since the messages that is sent and received is a shared observablebuffer[string] we must filter it according to the group unique name
  def setGroupChatMessage(messageList : ListView[String]) : Unit = {
    groupChatMessages.items = messageList.getItems

  }

  def setName(name : String) : Unit = {
    userName = name
  }

// this is the request message sent to the chatclient which will run the
// updateGroupChatMessages in the mainwindowcontroller for each user in group chat

  def updateGroupChatMessage() : Unit = {
    var key : String = ""
    groupChatList.getItems.foreach{
      user => key += user.name
    }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val currentDateTime = LocalDateTime.now()
    val formattedDateTime = currentDateTime.format(formatter)
    var text =  textMessage.getText()
    var message = s"$userName at $formattedDateTime\n$text"
    //get each user actor ref and do the SendGroupMessage to chatclient

    groupChatList.getItems.foreach {
      user => user.ref ! ChatClient.SendGroupMessage(message,key)
    }
  }



}
