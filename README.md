kanjiryoku
==========

A kanji practice game inspired by 漢字力診断.

The handwriting recognition engine used under the hood is Zinnia (http://zinnia.sourceforge.net/src), with SWIG-generated JNI bindings.
The Zinnia repository on SourceForge does not provide a 64-bit DLL, so you will have to compile one yourself, should you wish to run the software using a 64-bit JVM on Windows.

Problem data is not included for copyright reasons.

Please refer to the `README.md` file included with the `server` module for instructions on how to get the server up and running.