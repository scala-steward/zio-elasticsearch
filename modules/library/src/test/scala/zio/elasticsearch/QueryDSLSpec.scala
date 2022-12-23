package zio.elasticsearch

import zio.Scope
import zio.elasticsearch.ElasticQuery._
import zio.elasticsearch.utils._
import zio.test.Assertion.equalTo
import zio.test._

object QueryDSLSpec extends ZIOSpecDefault {
  override def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("Query DSL")(
      suite("creating ElasticQuery")(
        test("successfully create Match query using `matches` method") {
          val queryString = matches(field = "day_of_week", value = "Monday")
          val queryBool   = matches(field = "day_of_week", value = true)
          val queryLong   = matches(field = "day_of_week", value = 1L)

          assert(queryString)(equalTo(Match(field = "day_of_week", value = "Monday")))
          assert(queryBool)(equalTo(Match(field = "day_of_week", value = true)))
          assert(queryLong)(equalTo(Match(field = "day_of_week", value = 1)))
        },
        test("successfully create `Must` query from two Match queries") {
          val query = boolQuery()
            .must(matches(field = "day_of_week", value = "Monday"), matches(field = "customer_gender", value = "MALE"))

          assert(query)(
            equalTo(
              BoolQuery(
                must = List(
                  Match(field = "day_of_week", value = "Monday"),
                  Match(field = "customer_gender", value = "MALE")
                ),
                should = List.empty
              )
            )
          )
        },
        test("successfully create `Should` query from two Match queries") {
          val query = boolQuery()
            .should(
              matches(field = "day_of_week", value = "Monday"),
              matches(field = "customer_gender", value = "MALE")
            )

          assert(query)(
            equalTo(
              BoolQuery(
                must = List.empty,
                should = List(
                  Match(field = "day_of_week", value = "Monday"),
                  Match(field = "customer_gender", value = "MALE")
                )
              )
            )
          )
        },
        test("successfully create `Must/Should` mixed query") {
          val query = boolQuery()
            .must(matches(field = "day_of_week", value = "Monday"), matches(field = "customer_gender", value = "MALE"))
            .should(matches(field = "customer_age", value = 23))

          assert(query)(
            equalTo(
              BoolQuery(
                must = List(
                  Match(field = "day_of_week", value = "Monday"),
                  Match(field = "customer_gender", value = "MALE")
                ),
                should = List(Match(field = "customer_age", value = 23))
              )
            )
          )
        },
        test("successfully create `Should/Must` mixed query") {
          val query = boolQuery()
            .must(matches(field = "customer_age", value = 23))
            .should(
              matches(field = "day_of_week", value = "Monday"),
              matches(field = "customer_gender", value = "MALE")
            )

          assert(query)(
            equalTo(
              BoolQuery(
                must = List(Match(field = "customer_age", value = 23)),
                should =
                  List(Match(field = "day_of_week", value = "Monday"), Match(field = "customer_gender", value = "MALE"))
              )
            )
          )
        },
        test("successfully create Exists Query") {
          val query = exists(field = "day_of_week")

          assert(query)(equalTo(Exists(field = "day_of_week")))
        },
        test("successfully create MatchAll Query") {
          val query = matchAll()

          assert(query)(equalTo(MatchAll()))
        },
        test("successfully create empty Range Query") {
          val query = range(field = "customer_age")

          assert(query)(equalTo(Range(field = "customer_age", lower = Unbounded, upper = Unbounded)))
        },
        test("successfully create Range Query with upper bound") {
          val query = range(field = "customer_age").lt(23)

          assert(query)(equalTo(Range(field = "customer_age", lower = Unbounded, upper = LessThan(23))))
        },
        test("successfully create Range Query with lower bound") {
          val query = range(field = "customer_age").gt(23)

          assert(query)(equalTo(Range(field = "customer_age", lower = GreaterThan(23), upper = Unbounded)))
        },
        test("successfully create Range Query with inclusive upper bound") {
          val query = range(field = "customer_age").lte(23)

          assert(query)(equalTo(Range(field = "customer_age", lower = Unbounded, upper = LessThanOrEqualTo(23))))
        },
        test("successfully create Range Query with inclusive lower bound") {
          val query = range(field = "customer_age").gte(23)

          assert(query)(equalTo(Range(field = "customer_age", lower = GreaterThanOrEqualTo(23), upper = Unbounded)))
        },
        test("successfully create Range Query with both upper and lower bound") {
          val query = range(field = "customer_age").gte(23).lt(50)

          assert(query)(equalTo(Range(field = "customer_age", lower = GreaterThanOrEqualTo(23), upper = LessThan(50))))
        }
      ),
      suite("encoding ElasticQuery containing `Match` leaf query as JSON")(
        test("properly encode Match query") {
          val query = matches(field = "day_of_week", value = true)
          val expected =
            """
              |{
              |  "query": {
              |    "match": {
              |      "day_of_week": true
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Bool Query with Must containing `Match` leaf query") {
          val query = boolQuery().must(matches(field = "day_of_week", value = "Monday"))
          val expected =
            """
              |{
              |  "query": {
              |    "bool": {
              |      "must": [
              |        {
              |          "match": {
              |            "day_of_week": "Monday"
              |          }
              |        }
              |      ],
              |      "should": []
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Bool Query with Should containing `Match` leaf query") {
          val query = boolQuery().should(matches(field = "day_of_week", value = "Monday"))
          val expected =
            """
              |{
              |  "query": {
              |    "bool": {
              |      "must": [],
              |      "should": [
              |        {
              |          "match": {
              |            "day_of_week": "Monday"
              |          }
              |        }
              |      ]
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Bool Query with both Must and Should containing `Match` leaf query") {
          val query = boolQuery()
            .must(matches(field = "customer_id", value = 1))
            .should(matches(field = "day_of_week", value = "Monday"))
          val expected =
            """
              |{
              |  "query": {
              |    "bool": {
              |      "must": [
              |        {
              |          "match": {
              |            "customer_id": 1
              |          }
              |        }
              |      ],
              |      "should": [
              |        {
              |          "match": {
              |            "day_of_week": "Monday"
              |          }
              |        }
              |      ]
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Exists Query") {
          val query = exists(field = "day_of_week")
          val expected =
            """
              |{
              |  "query": {
              |    "exists": {
              |      "field": "day_of_week"
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode MatchAll Query") {
          val query = matchAll()
          val expected =
            """
              |{
              |  "query": {
              |    "match_all": {}
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Unbounded Range Query") {
          val query = range(field = "field")
          val expected =
            """
              |{
              |  "query": {
              |    "range": {
              |      "field": {
              |      }
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Range Query with Lower Bound") {
          val query = range(field = "customer_age").gt(23)
          val expected =
            """
              |{
              |  "query": {
              |    "range": {
              |      "customer_age": {
              |        "gt": 23
              |      }
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Range Query with Upper Bound") {
          val query = range(field = "customer_age").lt(23)
          val expected =
            """
              |{
              |  "query": {
              |    "range": {
              |      "customer_age": {
              |        "lt": 23
              |      }
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Range Query with Inclusive Lower Bound") {
          val query = range(field = "expiry_date").gte("now")
          val expected =
            """
              |{
              |  "query": {
              |    "range": {
              |      "expiry_date": {
              |        "gte": "now"
              |      }
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Range Query with inclusive Upper Bound") {
          val query = range(field = "customer_age").lte(100L)
          val expected =
            """
              |{
              |  "query": {
              |    "range": {
              |      "customer_age": {
              |        "lte": 100
              |      }
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        },
        test("properly encode Range Query with both Upper and Lower Bound") {
          val query = range(field = "customer_age").gte(10).lt(100)
          val expected =
            """
              |{
              |  "query": {
              |    "range": {
              |      "customer_age": {
              |        "gte": 10,
              |        "lt": 100
              |      }
              |    }
              |  }
              |}
              |""".stripMargin

          assert(query.toJsonBody)(equalTo(expected.toJson))
        }
      )
    )
}