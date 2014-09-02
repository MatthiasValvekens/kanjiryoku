kanjiryoku
==========

A kanji practice game inspired by 漢字力診断.

The handwriting recognition engine used under the hood is Zinnia (http://zinnia.sourceforge.net/src), with SWIG-generated JNI bindings.
The Zinnia repository on SourceForge does not provide 64-bit DLL, so you will have to compile one yourself should you wish to run the software through a 64-bit JVM.

The input files for SWIG are included with the Zinnia source.
