import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.ReachabilityEvent
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe

// https://developer.lightbend.com/guides/akka-sample-cluster-scala/
object ClusterListener {

    sealed trait Event
    // internal adapted cluster events only
    private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent) extends Event
    private final case class MemberChange(event: MemberEvent) extends Event

    def apply(): Behavior[Event] = Behaviors.setup { context =>
        val memberEventAdapter: ActorRef[MemberEvent] = context.messageAdapter(MemberChange)
        Cluster(context.system).subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

        val reachabilityAdapter = context.messageAdapter(ReachabilityChange)
        Cluster(context.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

        Behaviors.receiveMessage { message =>
            message match {
                case ReachabilityChange(reachabilityEvent) =>
                    reachabilityEvent match {
                        case UnreachableMember(member) =>
                            context.log.info("Member detected as unreachable: {}", member)
                        case ReachableMember(member) =>
                            context.log.info("Member back to reachable: {}", member)
                    }

                case MemberChange(changeEvent) =>
                    changeEvent match {
                        case MemberUp(member) =>
                            context.log.info("Member is Up: {}", member.address)
                        case MemberRemoved(member, previousStatus) =>
                            context.log.info("Member is Removed: {} after {}",
                                member.address, previousStatus)
                        case _: MemberEvent => // ignore
                    }
            }
            Behaviors.same
        }
    }
}