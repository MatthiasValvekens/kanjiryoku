kanjiryoku
==========

A kanji practice game inspired by 漢字力診断.

The handwriting recognition engine used under the hood is Zinnia (http://zinnia.sourceforge.net/src), with SWIG-generated JNI bindings.
The Zinnia repository on SourceForge does not provide a 64-bit DLL, so you will have to compile one yourself, should you wish to run the software using a 64-bit JVM.

The input files for SWIG are included with the Zinnia source. You should put the generated Java files in the "org.chasen.crfpp" package.
I'll add a tutorial on how to use Maven's SWIG plugin to build the dependency for any platform some time in the future.

Problem data is not included for copyright reasons.

(The game is technically playable now, but hardly interesting)
