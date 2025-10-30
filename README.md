[![version](https://img.shields.io/github/license/texttechnologylab/heideltime)]()
<a 
  href="https://github.com/texttechnologylab/heideltime/releases/latest"><img 
  alt="GitHub Latest Release"
  src="https://img.shields.io/github/v/release/texttechnologylab/heideltime?link=https%3A%2F%2Fgithub.com%2Ftexttechnologylab%heideltime%2Freleases%2Flatest"></a>
<a
  href="https://github.com/texttechnologylab/heideltime/packages/2650783"><img
  alt="Package"
  src="https://img.shields.io/github/v/release/texttechnologylab/heideltime?label=Package&color=ab7df8"></a>
[![](https://jitpack.io/v/texttechnologylab/heideltime.svg)](https://jitpack.io/#texttechnologylab/heideltime)
[![paper](https://img.shields.io/badge/paper-ACL--anthology-B31B1B.svg)](http://www.lrec-conf.org/proceedings/lrec2022/pdf/2022.lrec-1.505.pdf)


## About TTLab's Extension of HeidelTime
HeidelTime is one of the most widespread and successful tools for detecting temporal expressions in texts. Since HeidelTime's pattern matching system is based on regular expression, it can be extended in a convenient way. We present such an extension for the German resources of HeidelTime: HeidelTimeext. The extension has been brought about by means of observing false negatives within real world texts and various time banks. The gain in coverage is 2.7 % or 8.5 %, depending on the admitted degree of potential overgeneralization. We describe the development of HeidelTimeext, its evaluation on text samples from various genres, and share some linguistic observations.

### How to Cite

> Andy Luecking, Manuel Stoeckel, Giuseppe Abrami, and Alexander Mehler. 2022. [I still have Time(s): Extending HeidelTime for German Texts](https://aclanthology.org/2022.lrec-1.505/). In _Proceedings of the Thirteenth Language Resources and Evaluation Conference_, pages 4723–4728, Marseille, France. European Language Resources Association. [[PDF]](https://aclanthology.org/2022.lrec-1.505.pdf)

### BibTex
```
@inproceedings{luecking-etal-2022-still,
    title = "{I} still have Time(s): Extending {H}eidel{T}ime for {G}erman Texts",
    author = "Luecking, Andy  and
      Stoeckel, Manuel  and
      Abrami, Giuseppe  and
      Mehler, Alexander",
    editor = "Calzolari, Nicoletta  and
      B{\'e}chet, Fr{\'e}d{\'e}ric  and
      Blache, Philippe  and
      Choukri, Khalid  and
      Cieri, Christopher  and
      Declerck, Thierry  and
      Goggi, Sara  and
      Isahara, Hitoshi  and
      Maegaard, Bente  and
      Mariani, Joseph  and
      Mazo, H{\'e}l{\`e}ne  and
      Odijk, Jan  and
      Piperidis, Stelios",
    booktitle = "Proceedings of the Thirteenth Language Resources and Evaluation Conference",
    month = jun,
    year = "2022",
    address = "Marseille, France",
    publisher = "European Language Resources Association",
    url = "https://aclanthology.org/2022.lrec-1.505/",
    pages = "4723--4728",
    abstract = "HeidelTime is one of the most widespread and successful tools for detecting temporal expressions in texts. Since HeidelTime{'}s pattern matching system is based on regular expression, it can be extended in a convenient way. We present such an extension for the German resources of HeidelTime: HeidelTimeExt. The extension has been brought about by means of observing false negatives within real world texts and various time banks. The gain in coverage is 2.7 {\%} or 8.5 {\%}, depending on the admitted degree of potential overgeneralization. We describe the development of HeidelTimeExt, its evaluation on text samples from various genres, and share some linguistic observations. HeidelTimeExt can be obtained from \url{https://github.com/texttechnologylab/heideltime}."
}
```

## Maven


### Via [GitHub Packages](https://docs.github.com/en/packages)

Requires Maven to be set-up for [authentication with GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages).

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/texttechnologylab/*</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>org.texttechnologylab</groupId>
    <artifactId>heideltime</artifactId>
    <version>4.0.4</version>
  </dependency>
</dependencies>

<!-- Authentication can also be set-up in your ~/.m2/settings.xml file -->
<servers>
  <server>
    <id>github</id>
    <username>USERNAME</username>
    <password>TOKEN</password>
  </server>
</servers>
```

### Via [JitPack](https://jitpack.io/)

Add the JitPack repository and the dependency to your pom.xml:
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.texttechnologylab</groupId>
    <artifactId>heideltime</artifactId>
    <version>4.0.4</version>
  </dependency>
</dependencies>
```

## Original HeidelTime

**HeidelTime** is a multilingual, domain-sensitive temporal tagger developed at the [Database Systems Research Group](http://dbs.ifi.uni-heidelberg.de/) at [Heidelberg University](http://www.uni-heidelberg.de/index_e.html). It extracts temporal expressions from documents and normalizes them according to the TIMEX3 annotation standard. HeidelTime is available as [UIMA](http://uima.apache.org/) annotator and as standalone version.

**HeidelTime** currently contains hand-crafted resources for **13 languages**: English, German, Dutch, Vietnamese, Arabic, Spanish, Italian, French, Chinese, Russian, Croatian, Estonian and Portuguese. In addition, starting with version 2.0, HeidelTime contains **automatically created resources for more than 200 languages**. Although these resources are of lower quality than the manually created ones, temporal tagging of many of these languages has never been addressed before. Thus, HeidelTime can be used as a baseline for temporal tagging of all these languages or as a starting point for developing temporal tagging capabilities for them. 

**HeidelTime** distinguishes between **news-style** documents and **narrative-style documents** (e.g., Wikipedia articles) in all languages. In addition, English colloquial (e.g., Tweets and SMS) and scientific articles (e.g., clinical trails) are supported.

Original **HeidelTime** can be obtained at [github](https://github.com/HeidelTime/heideltime).

Want to see what it can do before you delve in? Take a look at **HeidelTime**'s **[online demo](http://heideltime.ifi.uni-heidelberg.de/heideltime/)**.
