package hokko.core

import hokko.syntax.BehaviorSyntax
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import scalaz.syntax.applicative._

class DiscreteBehaviorTest extends FRPTestSuite with BehaviorSyntax {
  describe("DiscreteBehaviors") {
    describe("that are constant") {
      val const = DiscreteBehavior.constant(5)
      it("should always return the same value and its changes should never have occurrences") {
        val occurrences = mkOccurrences(const.changes) { implicit engine =>
          val currentValues = engine.askCurrentValues()
          assert(currentValues(const).get.value === 5)
        }
        assert(occurrences === List.empty)
      }
    }
    describe("made from polling and changes") {
      var x = 0
      val src = Event.source[Unit]
      val b = Behavior.fromPoll(() => x)
      val db = b.signalChanges(src)
      it("should retrieve it's current value from the poll at the signaled time") {
        val occurrences = mkOccurrences(db.changes) { implicit engine =>
          x = 5
          engine.fire(src -> ())
          val currentValues = engine.askCurrentValues()
          assert(currentValues(b).get.value === 5)
        }
        assert(occurrences === List(5))
      }
    }
    describe("that are reverse applied ") {
      val src = Event.source[Int]
      val bParam = src.fold(0) { (acc, n) => n }

      it("to constant functions should simply apply the functon and have its results on .changes") {
        val const = DiscreteBehavior.constant((_: Int) * 2)
        val bApplied = bParam.reverseApply(const)
        check { (ints: List[Int]) =>
          val occs = mkOccurrences(bApplied.changes) { implicit engine =>
            fireAll(src, ints)
            val currentValues = engine.askCurrentValues()
            currentValues(bApplied).get.value == ints.lastOption.map(_ * 2).getOrElse(0)
          }
          occs == ints.map(_ * 2)
        }
      }

      it("to changing functions should simply apply the functon and have its results on .changes") {
        val bPoorMansDouble = bParam.map { i => (int: Int) => int + i }
        val bApplied = bParam.reverseApply(bPoorMansDouble)
        check { (ints: List[Int]) =>
          val occs = mkOccurrences(bApplied.changes) { implicit engine =>
            fireAll(src, ints)
            val currentValues = engine.askCurrentValues()
            currentValues(bApplied).get.value == ints.lastOption.map(_ * 2).getOrElse(0)
          }
          occs == ints.map(_ * 2)
        }
      }
    }
  }
}
