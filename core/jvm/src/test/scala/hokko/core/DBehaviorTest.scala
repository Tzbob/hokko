package hokko.core

import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import cats.syntax.all._
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

class DBehaviorTest extends FunSuite with FRPSuite with Checkers {
  test("Constant DBehaviors return constants and don't have occurences") {
    val const = DBehavior.constant(5)
    val occurrences = mkOccurrences(const.changes) { implicit engine =>
      val currentValues = engine.askCurrentValues()
      assert(currentValues(const.toCBehavior).get === 5)
    }
    assert(occurrences === List.empty)
  }

  val src = Event.source[Int]
  val bParam: DBehavior[Int] = src
    .fold(0) { (acc, n) =>
      n
    }
    .toDBehavior

  test("DBehaviors can be applied") {
    val bPoorMansDouble = bParam.map { (i: Int) => (int: Int) =>
      int + i
    }
    val bApplied = bPoorMansDouble ap bParam
    check { (ints: List[Int]) =>
      val occs = mkOccurrences(bApplied.changes) { implicit engine =>
        fireAll(src, ints)
        val currentValues = engine.askCurrentValues()
        currentValues(bApplied.toCBehavior).get == ints.lastOption
          .map(_ * 2)
          .getOrElse(0)
      }
      occs == ints.map(_ * 2)
    }
  }

  test("DBehaviors can be diffed into IBehaviors") {
    val ib = bParam.toIBehavior(_ - _)(_ + _)
    check { (ints: List[Int]) =>
      val occurrences = mkOccurrencesWithPulses(ib.deltas)(src, ints)
      val expected = (0 +: ints, (0 +: ints).tail).zipped.map { (o, n) =>
        n - o
      }
      occurrences == expected
    }
  }
}
