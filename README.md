# nasa-background
A Kotlin console app to fetch the latest image from NASA's [Earth imaging camera](https://epic.gsfc.nasa.gov/) 
aboard the [DISCOVR](https://www.nesdis.noaa.gov/content/dscovr-deep-space-climate-observatory/) satellite .

Makes completely overkill usage of [Vertx](https://vertx.io) and Kotlin's coroutines because I want to play with them.

Also uses [Gson](https://github.com/google/gson) for Json serialization 
and [Clikt](https://github.com/ajalt/clikt) for command line parsing.

**Note: This is play code not professional. So there is minimal documentation and no tests**
