# SiteCrawler

This project is dedicated to testing the features and behaviors of
[crawler4j](https://github.com/yasserg/crawler4j).

## How to Compile

Compile and package with Maven 3

```
$ mvn clean package
```

## How to Run (short ver.)

```
$ java -jar ./site/target/site-0.1.0.jar &
$ java -jar ./crawler/target/crawler-0.1.0.jar 'http://localhost:8080/'
```

## How to Run (longer ver.)

### Site

Start the simple website.

#### Syntax

```
$ java -jar ./site/target/site-0.1.0.jar
```

It will have the following default user accounts:

| Username | Password |
|----------|----------|
| user1    | pass1    |
| user2    | pass2    |
| user3    | pass3    |

Or, specify different ones in a separate configuration file.

#### Optional System Properties

| Name      | Default Value |
|-----------|---------------|
| conf.path |               |

#### Example

```
$ mkdir ./test
$ echo 'site.users=[{"username":"tester","password":"testing","roles":"USER"}]' > ./test/site.conf
$ java -Dconf.path=file:test/site.conf -jar ./site/target/site-0.1.0.jar
```

### Crawler

Run the crawler

#### Syntax

```
$ java -jar ./crawler/target/crawler-0.1.0.jar [ URL [ URL [ URL ... ] ] ]
```

### Optional System Properties

| Name      | Default Value |
|-----------|---------------|
| conf.path |               |
| instances | 1             |

#### Example

```
$ java -Dconf.path=path/to/conf -Dinstances=1 \
  -jar ./crawler/target/crawler-0.1.0.jar \
  'http://localhost:8080/'
```
