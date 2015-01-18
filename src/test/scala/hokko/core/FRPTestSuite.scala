package hokko.core

import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import org.scalatest.FunSpec
import org.scalatest.prop.Checkers
import scala.collection.mutable.ListBuffer
import scala.language.{ existentials, reflectiveCalls }

trait FRPTestSuite extends FunSpec with Checkers {
  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSize = 10)

  def mkOccurrences[A](ev: Event[A])(performSideEffects: Engine => Unit): List[A] = {
    val engine = Engine.compile(ev)()
    val occurrences = ListBuffer.empty[A]
    val subscription = engine.subscribeForPulses {
      _(ev).foreach(occurrences += _)
    }
    performSideEffects(engine)
    subscription.cancel()
    occurrences.toList
  }

  def fireAll[A](src: EventSource[A], pulses: List[A])(implicit engine: Engine) =
    pulses.foreach(i => engine.fire(src -> i))

  def mkOccurrencesWithPulses[A, B](target: Event[B])(src: EventSource[A], pulses: List[A]) =
    mkOccurrences(target) { implicit engine => fireAll(src, pulses) }

}