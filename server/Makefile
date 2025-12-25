SBT ?= sbt

.PHONY: clean
clean:
	$(SBT) clean

.PHONY: lint
lint:
	$(SBT) scalafmtCheckAll
	$(SBT) 'scalafix --check'

.PHONY: fmt
fmt:
	$(SBT) scalafmtCheckAll || $(SBT) scalafmtAll
	$(SBT) scalafmtSbtCheck || $(SBT) scalafmtSbt
	$(SBT) scalafix

.PHONY: test
test:
	$(SBT) -batch test

.PHONY: compile
compile:
	$(SBT) compile

.PHONY: package
package:

.PHONY: run
run:
	$(SBT) run