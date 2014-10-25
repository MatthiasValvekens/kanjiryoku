Kanjiryoku server configuration
===============================

(NOTE: This information is no longer accurate. It will be updated in the near future)

The kanjiryoku server registers clients, manages sessions and broadcasts problems during game sessions. It is also fully responsible for character recognition and answer checking. Hence, assuming the standard Zinnia character recognition engine is used, the native libraries are only required to run the server. Dropping the relevant library files in the same folder as the server `.jar` should do the trick.

The configuration file (`kanjiryoku.cfg`) is a JSON text file and should be placed in the same folder.

Names and default values of all keys can be found in the `server` module's `ConfigFields` class in the `be.mapariensis.kanjiryoku.config` package.

Top-level keys
==============
- `port`: This key is mandatory, and specifies the port number to listen on.
- `workerThreads`: Specifies the number of worker threads used to process commands sent to the server. Threads are rotated between connections to save resources, so a connection does not hog worker resources while it's idle. A worker is returned to the pool as soon as it finishes executing a command. The default value of this parameter is 10. Increase/decrease in accordance with available resources and desired performance.
- `workerBufferSize`: Specifies the amount of buffer memory allocated to each worker thread, in bytes. The default value of this parameter is 2048, but this may increase when the need arises. Fair warning: the server currently doesn't support fragmented messaging (it will soon, hopefully), so fiddling with this parameter is probably not a good idea.
- `engine`: This key's value is another JSON object. It is passed in its entirety to the `KanjiGuesserFactory` implementation supplied. See further down.
- `games`: Contains game-specific settings. See further down.

Character recognition settings
===============================

The `engine` JSON object has one required key: `guesserFactoryClassName`, which you should probably set to `be.mapariensis.kanjiryoku.cr.ZinniaGuesser$Factory`, unless you rolled your own character recognition engine and/or you're reading a horribly outdated version of this document. All other parameters are for the `KanjiGuesserFactory` to decide.

Zinnia requires a model file path (`writingModelPath`), but otherwise has no required parameters. Zinnia's tolerance can also be fine-tuned through the `answerTolerance` parameter. Basically, higher values correspond to lower difficulty. The default value is 50, which should be plenty, but is still low enough to provide a challenging game experience.

Game settings
=============
The `games` JSON object contains a JSON object for every game the server supports. Since the server only runs turn-based guessing games (key `TAKINGTURNS`) at the time of writing, this paragraph will be about the settings for `TAKINGTURNS`.

- `categories`: This key takes a JSON array of strings, and specifies the names of the different problem categories to be used by the server. In principle, these names are never shown to the user, so they do not have to be human-readable. The order of the names specifies the order they will be used in when setting problems.
- `minimalDifficulty` and `maximalDifficulty`: What it says on the tin. Problems have an associated difficulty level, and these parameters set the bounds for that value. Defaults are 1 and 11, respectively.
- `problemsPerCategory`: Specifies the number of problems that should be used, per category. Default value is 5. If you specify two categories `yomi` and `kaki`, the software will ask 5 questions from `yomi`, and then 5 questions from `kaki`.
- `filePathPattern`: Specifies the filename(s) to look for problems. It is a parametrized format string that takes two input variables: `category` and `difficulty`. For example, using the category settings from before, the format string `my_${category}_${difficulty}.txt` would read files `my_yomi_01.txt` through `my_kaki_11.txt` and assign the problems to the appropriate categories. You should separate problems by difficulty and category for maximal ease of use. The format of the `difficulty` literal is controlled by the `difficultyFormat` parameter, which takes the standard value `%02d` when left unspecified (use a standard `printf`-style format string).
- `resetDifficultyAfterCategorySwitch`: As the name implies, this switch controls whether difficulty is reset when switching categories during a game.


Sample minimal config file
================
```json
{
	"port": 1000,
	"engine": {"writingModelPath": "data\\writingmodel\\handwriting-ja.model", "guesserFactoryClassName": "be.mapariensis.kanjiryoku.cr.ZinniaGuesser$Factory"},
	"games" : 
		{
			"TAKINGTURNS" :
				{
					"filePathPattern" : "data\\problems\\my_${category}_${difficulty}.txt",
					"categories": ["yomi","kaki"]
				}
		}
}
```

Problem definition language
============================
Coming soon