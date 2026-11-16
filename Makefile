APP_NAME     ?= HabitTracker
APP_VERSION  ?= 1.0.0
MAIN_CLASS   ?= Main
JAVA_RELEASE ?= 17

JAVAC    ?= javac
JAVA     ?= java
JAR      ?= jar
JPACKAGE ?= jpackage

SRC       := $(wildcard *.java)
LIBS      := $(wildcard lib/*.jar)
LIB_NAMES := $(notdir $(LIBS))
empty     :=
space     := $(empty) $(empty)
JAR_CP    := $(subst $(space), ,$(addprefix lib/,$(LIB_NAMES)))

BIN        := bin
BUILD      := build
DIST       := dist
INSTALLERS := $(BUILD)/installers
MANIFEST   := $(BUILD)/MANIFEST.MF
JAR_FILE   := $(DIST)/$(APP_NAME).jar

JAVAC_FLAGS := -encoding UTF-8 --release $(JAVA_RELEASE)

.PHONY: default build run jar package package-deb clean help

default: run

build:
	rm -rf $(BIN)
	mkdir -p $(BIN)
	$(JAVAC) $(JAVAC_FLAGS) -d $(BIN) -cp "lib/*" $(SRC)

run: build
	$(JAVA) -cp "$(BIN):lib/*" $(MAIN_CLASS)

jar: build
	rm -rf $(DIST)
	mkdir -p $(DIST)/lib $(BUILD)
	cp $(LIBS) $(DIST)/lib/
	printf 'Main-Class: %s\nClass-Path: %s\nImplementation-Version: %s\n' \
		'$(MAIN_CLASS)' '$(JAR_CP)' '$(APP_VERSION)' > $(MANIFEST)
	$(JAR) --create --file $(JAR_FILE) --manifest $(MANIFEST) -C $(BIN) .

package: jar
	rm -rf $(INSTALLERS)/$(APP_NAME)
	mkdir -p $(INSTALLERS)
	$(JPACKAGE) --type app-image \
		--name $(APP_NAME) \
		--app-version $(APP_VERSION) \
		--input $(DIST) \
		--main-jar $(notdir $(JAR_FILE)) \
		--main-class $(MAIN_CLASS) \
		--dest $(INSTALLERS) \
		--vendor "Habit Tracker" \
		--description "Habit tracker with stats, journal, and focus timer"

package-deb: jar
	@command -v dpkg-deb >/dev/null 2>&1 || { \
		echo "dpkg-deb not found. Install: sudo apt install dpkg-dev"; exit 1; }
	@command -v fakeroot >/dev/null 2>&1 || { \
		echo "fakeroot not found. Install: sudo apt install fakeroot"; exit 1; }
	rm -f $(INSTALLERS)/*.deb
	mkdir -p $(INSTALLERS)
	$(JPACKAGE) --type deb \
		--name $(APP_NAME) \
		--app-version $(APP_VERSION) \
		--input $(DIST) \
		--main-jar $(notdir $(JAR_FILE)) \
		--main-class $(MAIN_CLASS) \
		--dest $(INSTALLERS) \
		--vendor "Habit Tracker" \
		--description "Habit tracker with stats, journal, and focus timer" \
		--linux-shortcut

clean:
	rm -rf $(BIN) $(BUILD) $(DIST)

help:
	@printf '%s\n' \
		'Targets:' \
		'  make / make run   compile and launch' \
		'  make build        compile only' \
		'  make jar          build dist/HabitTracker.jar + lib/' \
		'  make package      build a runnable app image' \
		'  make package-deb  build a .deb (needs dpkg-deb + fakeroot)' \
		'  make clean        remove bin, build, dist'
