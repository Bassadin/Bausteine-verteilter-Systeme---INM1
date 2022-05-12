import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus
import akka.cluster.typed.{Cluster, Subscribe}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object ClusterListener {

    sealed trait Event
    // internal adapted cluster events only
    private final case class ReachabilityChange(
        reachabilityEvent: ReachabilityEvent
    ) extends Event
    private final case class MemberChange(event: MemberEvent) extends Event

    implicit val timeout: Timeout = 3.seconds

    def apply(): Behavior[Event] = Behaviors.setup { context =>
        val cluster = Cluster(context.system)

        implicit val timeout: Timeout = 3.seconds

        val memberEventAdapter: ActorRef[MemberEvent] =
            context.messageAdapter(MemberChange)
        cluster.subscriptions ! Subscribe(
          memberEventAdapter,
          classOf[MemberEvent]
        )

        val reachabilityAdapter = context.messageAdapter(ReachabilityChange)
        cluster.subscriptions ! Subscribe(
          reachabilityAdapter,
          classOf[ReachabilityEvent]
        )

        Behaviors.receiveMessage { message =>
            message match {
                case ReachabilityChange(reachabilityEvent) =>
                    reachabilityEvent match {
                        case UnreachableMember(member) =>
                            context.log.info(
                              "Member detected as unreachable: {}",
                              member
                            )
                        case ReachableMember(member) =>
                            context.log.info(
                              "Member back to reachable: {}",
                              member
                            )
                    }

                case MemberChange(changeEvent) =>
                    changeEvent match {
                        case MemberUp(member) =>
                            context.log.info("Member is Up: {}", member.address)

                            val members = cluster.state.members.filter(
                              _.status == MemberStatus.Up
                            )

                            if (members.size >= 4) {
                                context.ask(
                                  context.system.receptionist,
                                  Find(ParseFileActor.serviceKey)
                                ) { case Success(listing: Listing) =>
                                    val instances =
                                        listing.allServiceInstances(
                                          ParseFileActor.serviceKey
                                        )
                                    val parseFileActorRef = instances.head

                                    parseFileActorRef ! ParseFileActor
                                        .LoadDataFromFileAndGetParseActor(
                                          "./test_ticks.csv"
                                        )


                                }
                            }
                        case MemberRemoved(member, previousStatus) =>
                            context.log.info(
                              "Member is Removed: {} after {}",
                              member.address,
                              previousStatus
                            )
                        case _: MemberEvent => // ignore
                    }
            }
            Behaviors.same
        }
    }
}
