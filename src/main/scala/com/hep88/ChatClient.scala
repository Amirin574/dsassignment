package com.hep88
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import ChatClient.Command
import akka.actor.Address
import scalafx.application.Platform
import scalafx.collections.{ObservableBuffer, ObservableHashSet}
import com.hep88.Client
import scalafx.scene.control.ListView
import scalafx.Includes._

import scala.collection.mutable
//data structure
case class User(name: String, ref:ActorRef[ChatClient.Command])



object ChatClient {
  //chat client protocol
  sealed trait Command
  case object start extends Command

  //find the chat server
  final case object FindTheServer extends Command
  private case class ListingResponse(listing: Receptionist.Listing) extends Command

  //chat protocol
  case class Joined(lists: Iterable[User]) extends Command
  case class MemberList(lists: Iterable[User]) extends Command
  //The reason for two list is because, we will only send the joined message for 1 time
  //We have to keep the client updated about the new user joined

  case class StartJoin(name: String) extends Command
  case class SendMessageL(target: ActorRef[ChatClient.Command], content: String, user : String, time : String )extends Command
  case class Message(msg: String, from: ActorRef[ChatClient.Command], user : String, time : String) extends Command


  case class OpenWindow(user : List[User]) extends Command

  case class SendGroupMessage(msg : String, key : String) extends Command
  case class StartPrivateChat(user : List[User]) extends Command
  case class OpenPrivateChat(user : List[User]) extends Command
  case class SendPrivateMessage(msg : String, key : String) extends Command

  //chat client value
  var nameOpt: Option[String] = None
  var from : String = ""

  //Chat client hash set


  val members = new ObservableHashSet[User]()
  val unreachables = new ObservableHashSet[Address]()
  unreachables.onChange { (ns, _) =>
    Platform.runLater {
      Client.control.updateList(members.toList.filter(y => !unreachables.exists(x => x == y.ref.path.address)))
    }
  }

  members.onChange { (ns, _) =>
    Platform.runLater {
      Client.control.updateList(ns.toList.filter(y => !unreachables.exists(x => x == y.ref.path.address)))
    }
  }

  var remoteOpt:Option[ActorRef[ChatServer.Command]] = None
  var defaultBehaviour: Option[Behavior[ChatClient.Command]] = None

  def messageStarted(): Behavior[ChatClient.Command] = Behaviors.receive[ChatClient.Command]{
    (context, message) =>
      message match {
        case MemberList(list: Iterable[User])=>
          members.clear()
          members ++= list
          Behaviors.same

        case OpenWindow(user) =>
          Platform.runLater{
            Client.control.openGroupChat(user)
          }
          Behaviors.same
        case SendGroupMessage(msg, key) =>

          Platform.runLater {
            Client.control.updateGroupChatMessages(msg, key)
          }
          Behaviors.same
        case StartPrivateChat(users) =>
          users.foreach{
            user =>
              user.ref ! ChatClient.OpenPrivateChat(users)
          }
          Behaviors.same
        case OpenPrivateChat(users) =>
          Platform.runLater {
            Client.control.openPrivateChat(users)
          }
          Behaviors.same
        case SendPrivateMessage(msg, key) =>
          Platform.runLater {
            Client.control.updatePrivateChatMessages(msg, key)
          }
          Behaviors.same
      }
      Behaviors.same
  }.receiveSignal{
    case (context, PostStop) =>
      for (name <- nameOpt;
           remote <- remoteOpt) {
        remote ! ChatServer.Leave(name, context.self)
      }
      defaultBehaviour.getOrElse(Behaviors.same)

  }



  def apply(): Behavior[ChatClient.Command] =
    Behaviors.setup { context =>

      val listingAdapter: ActorRef[Receptionist.Listing] =
      context.messageAdapter { listing =>
        println(s"listingAdapter:listing: ${listing.toString}")
        ChatClient.ListingResponse(listing)
      }

      context.system.receptionist ! Receptionist.Subscribe(ChatServer.ServerKey, listingAdapter)

      defaultBehaviour = Option(Behaviors.receiveMessage { message =>
        message match {
          case ChatClient.start =>
            context.self ! FindTheServer
            Behaviors.same

          case FindTheServer =>
            println(s"Clinet Hello: got a FindTheServer message")
            context.system.receptionist !
              Receptionist.Find(ChatServer.ServerKey, listingAdapter)

            Behaviors.same

          case ListingResponse(ChatServer.ServerKey.Listing(listings)) =>
            val xs: Set[ActorRef[ChatServer.Command]] = listings
            for (x <- xs) {
              remoteOpt = Some(x)
            }
            Behaviors.same
          case Joined(list)=>
            Platform.runLater{
              Client.control.displayStatus("Joined")
            }
            members.clear()
            members ++= list
            messageStarted()

          case MemberList(list)=>
            println("User List")
            for(user <- list){
              println(user)
            }
            println("End of user list")
            Behaviors.same

          case StartJoin(name)=>
            nameOpt = Option(name)
            import com.hep88.ChatServer._
            for (remote <-remoteOpt) {
              remote ! JoinChat(nameOpt.get, context.self)
            }
            Behaviors.same
        }
      })
      defaultBehaviour.get
    }
}

