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
class PrivateChatWindowController(private val privateChatMessages : ListView[String], private val privateChatList : ListView[User],
                                  private val textMessage : TextField) {

//implements a similar controller to the group chat window controller
  //the difference is that the messages we are sending will be stored in a map containing observablebuffer[string] for private chat messages only

  var userName : String = ""

  def setPrivateChatList(users : ListView[User]) : Unit = {
    privateChatList.items = users.getItems

  }

  def setPrivateChatMessage(messageList : ListView[String]) : Unit = {
    privateChatMessages.items = messageList.getItems

  }

  def setName(name : String) : Unit = {
    userName = name
  }

  def updatePrivateChatMessage() : Unit = {
    var key : String = ""
    privateChatList.getItems.foreach{
      user => key += user.name
    }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val currentDateTime = LocalDateTime.now()
    val formattedDateTime = currentDateTime.format(formatter)
    var text =  textMessage.getText()
    var message = s"$userName at $formattedDateTime\n$text"
    //get each user actor ref and do the SendGroupMessage to chatclient

    privateChatList.getItems.foreach {
      user => user.ref ! ChatClient.SendPrivateMessage(message,key)
    }
  }







}