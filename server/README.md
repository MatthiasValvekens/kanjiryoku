Kanjiryoku server configuration
===============================

The kanjiryoku server registers clients, manages sessions and broadcasts problems during game sessions. It is also fully responsible for character recognition and answer checking. Hence, assuming the standard Zinnia character recognition engine is used, the native libraries are only required to run the server. Dropping the relevant library files in the same folder as the server `.jar` should do the trick.

The configuration file (`kanjiryoku.cfg`) is a JSON text file and should be placed in the same folder.

Names and default values of all keys can be found in the `server` module's `ConfigFields` class in the `be.mapariensis.kanjiryoku.config` package.

Top-level keys
==============
- `port`: This key is mandatory, and specifies the port number to listen on.
- `workerThreads`: Specifies the number of worker threads used to process commands sent to the server. Threads are rotated between connections to save resources, so a connection does not hog worker resources while it's idle. A worker is returned to the pool as soon as it finishes executing a command. The default value of this parameter is 10. Increase/decrease in accordance with available resources and desired performance.
- `workerBufferSize`: Specifies the amount of buffer memory allocated to each worker thread, in bytes. The default value of this parameter is 2048, but this may increase when the need arises.
Fair warning: while the server currently supports fragmented messaging it has not been subjected to stress tests, so fiddling with this parameter is still not a good idea.
- `engine`: This key's value is another JSON object. It is passed in its entirety to the `KanjiGuesserFactory` implementation supplied. See further down.
- `problemSets`: Controls the configuration of the problem set manager.
- `games`: Contains game-specific settings. See further down.
- `enableAdminCommands`: Experimental admin commands. These are undocumented, use at your own risk.

Character recognition settings
===============================

The `engine` JSON object has one required key: `guesserFactoryClassName`.
I recommend using the standard pooled guesser factory. You might want to just copy and paste from the sample configuration file.
The pooler guesser factory can be backed by any guesser factory implementation you prefer, but you probably want to use the Zinnia guesser, which is provided by `be.mapariensis.kanjiryoku.cr.ZinniaGuesser$Factory`.
Backend-specific settings should be passed in the `backendConfig` object, assuming you're using the standard pooled guesser.

Zinnia backend configuration
-----------------------------

Zinnia requires a model file path (`writingModelPath`), but otherwise has no required parameters. Zinnia's tolerance can also be fine-tuned through the `answerTolerance` parameter. Basically, higher values correspond to lower difficulty. The default value is 50, which should be plenty, but is still low enough to provide a challenging game experience.

Problem sets
============
The `problemSets` key takes a JSON object which serves as input to the problem set manager.
- `minimalDifficulty` and `maximalDifficulty`: What it says on the tin. Problems have an associated difficulty level, and these parameters set the bounds for that value. Defaults are 1 and 11, respectively.
- `problemsPerCategory`: Specifies the number of problems that should be used, per category.
Default value is 5.
If you specify two categories `yomi` and `kaki`, the software will ask 5 questions from `yomi`, and then 5 questions from `kaki`.
- `resetDifficultyAfterCategorySwitch`: As the name implies, this switch controls whether difficulty is reset when switching categories during a game.
The default setting is `true`.
- `fileEncoding`: Specify the default encoding of problem files.
This setting can be overridden in category-specific configuration.
- `filePathPattern`: Specifies the filename(s) to look for problems.
It is a parametrized format string that takes two input variables: `category` and `difficulty`.
For example, using categories `yomi` and `kaki`, with difficulty settings ranging from 1 to 11, the format string `problem_${category}_${difficulty}.txt` would read files `problem_yomi_01.txt` through `problem_kaki_11.txt` and assign the problems to the appropriate categories.
You should separate problems by difficulty and category for maximal ease of use.
The format of the `difficulty` literal is controlled by the `difficultyFormat` parameter, which takes the standard value `%02d` when left unspecified (use a standard `printf`-style format string).
This setting can be overridden in category-specific configuration.
- `categories`: The bulk of the interesting configuration takes place here.

Categories
-----------
Categories have names, determined by their key.
They model sets of different kinds of problems (reading/writing/yojijukugo as of now).
- The keys `fileEncoding` and `filePathPattern` can be used to override global settings specified at a higher level.
- `parserFactoryClassName`: Set this to `be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser$Factory` for normal reading and writing problems.
The yojijukugo problem parser is `be.mapariensis.kanjiryoku.providers.KanjiryokuYojiWrapper$Factory`.
- `parserFactoryConfig`: Is passed to the parser factory when constructing a parser.
This setting is not required for standard reading and writing problems.

Setting up dictionary access
------------------------------
Yojijukugo problems can be tricky to set up properly.
Since these involve selecting characters from a list of four possible choices, it makes sense to only present options that are somehow related to the correct character, otherwise they would be trivial to solve.
Hence, to make yojijukugo problems work as intended, the game needs access to a kanji dictionary.
This is done through the `KanjidicInterface` interface, and while you are free to implement this layer yourself, the game provides a standard implementation which reads data from a SQLite database file.

The structure of the database doesn't really matter, since the SQLite interface implementation expects you to put the queries in a separate file.
This was a conscious design decision, because there are many kanji dictionaries floating around on the web.
Most of these are copyrighted in some form or another, and I didn't want to bundle an entire dictionary with the application.
Hence, I felt that hardcoding the database schema would be a bad idea, so that's why I opted for user-specified queries.
As of now, this part of the code is not as clean as I would like it to be (mostly because JDBC has little to no support for named parameters), but I'm working on it.
Refer to the source code and sample config files for details.

Game settings
=============
The `games` JSON object contains a JSON object for every game the server supports. Since the server only runs turn-based guessing games (key `TAKINGTURNS`) at the time of writing, this paragraph will be about the settings for `TAKINGTURNS`.

- `categories`: This key takes a JSON array of strings, and specifies the names of the different problem categories to be used by the server.
In principle, these names are never shown to the user, so they do not have to be human-readable.
The order of the names specifies the order they will be used in when setting problems.
At runtime, these names are matched to keys specified in the `categories` object in `problemSets`.
- `enableBatonPass`: If set to `true`, problems will be rotated between players on skips until someone gets it right, or all players have had a turn, whichever comes first.
If `false`, a skip causes the server to drop the problem and broadcast a fresh one.
Default value is `false`.

Sample minimal config file
==========================
See `kanjiryoku.cfg.sample`.

Problem definition language
============================

A "given" word is marked as follows:
```
［reading］kanji
［ひと］人
［しょくひん］食品
```
For reading problems, the "test" word is marked as follows:
```
（reading）｛kanji｝
（ご）｛五｝
（しおり）｛栞｝
```
For writing problems, the "test" word is marked as follows:
```
｛reading｝（kanji）
｛まる｝（丸）
｛ほたる｝（蛍）
```

Some examples of reading problems:
```
（ほし）｛星｝が［ひか］光る
［かん］関（税）｛税｝［きょく］局
```

Some examples of writing problems:
```
［に］二｛そく｝（束）［さんもん］三文
［くちぎたな］口汚く｛ののし｝（罵）る
```

For the purposes of this discussion, yojijukugo problems are considered writing problems.