#!/bin/sh
"/Applications/WebStorm.app/Contents/jbr/Contents/Home/bin/java" -cp "/Applications/WebStorm.app/Contents/lib/externalProcess-rt.jar:/Applications/WebStorm.app/Contents/lib/app.jar:/Applications/WebStorm.app/Contents/lib/3rd-party-rt.jar" externalApp.nativessh.NativeSshAskPassApp "$@"
