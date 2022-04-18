#!/bin/sh
"/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home/bin/java" -cp "/Applications/IntelliJ IDEA.app/Contents/plugins/git4idea/lib/git4idea-rt.jar:/Applications/IntelliJ IDEA.app/Contents/lib/externalProcess-rt.jar:/Applications/IntelliJ IDEA.app/Contents/lib/app.jar:/Applications/IntelliJ IDEA.app/Contents/lib/3rd-party-rt.jar" git4idea.http.GitAskPassApp "$@"
