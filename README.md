## SHACTOR: Improving the Quality of Large-Scale Knowledge Graphs with Validating Shapes
### Demo Paper: SIGMOD-2023

> **ℹ Note:** This repository is a fork of the [original SHACTOR demo repository](https://github.com/dkw-aau/demo-shactor). It was created and extended by **Dominic Leidenfrost** as part of a **Bachelor's thesis at TU Wien**.
> 
> **Extensions include:**
> -  **ShEx (Shape Expressions) functionality** - Added support for ShEx schema generation alongside SHACL
> -  **Docker image** - Pre-built Docker Hub image for easy deployment
> -  **Environment variable configuration** - Flexible configuration via environment variables
> -  **Various quality-of-life improvements** - Enhanced usability and documentation

Read the paper: [https://dl.acm.org/doi/10.1145/3555041.3589723](https://dl.acm.org/doi/10.1145/3555041.3589723) or visit our website for more details: [https://relweb.cs.aau.dk/qse/shactor/](https://relweb.cs.aau.dk/qse/shactor/)

### Citing the work
Please cite us if you use the code in your project or publication

```bibtex
@inproceedings{DBLP:conf/sigmod/RabbaniLH23,
  author       = {Kashif Rabbani and
                  Matteo Lissandrini and
                  Katja Hose},
  title        = {{SHACTOR:} Improving the Quality of Large-Scale Knowledge Graphs with
                  Validating Shapes},
  booktitle    = {{SIGMOD} Conference Companion},
  pages        = {151--154},
  publisher    = {{ACM}},
  year         = {2023}
}
```

This readme contains all the necessary configuration to run the demo:

## Documentation

- **[DOCKER.md](DOCKER.md)** - Complete Docker setup guide (recommended for quick start)
- **[SETUP.md](SETUP.md)** - Manual setup instructions and detailed configuration

## Quick Start with Docker

**The easiest way to run SHACTOR is using our pre-built Docker image:**

```bash
docker pull dleidenfrost/shactor-app:latest

docker run -d \
  --name shactor \
  -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPARQL_ENDPOINT_URL=http://host.docker.internal:7200/ \
  -e SPARQL_REPOSITORY=LUBM-ScaleFactor-1 \
  dleidenfrost/shactor-app:latest
```

Then open http://localhost:8080 in your browser.

**For detailed Docker setup instructions, see [DOCKER.md](DOCKER.md).**

---

## Running the application (Manual Setup)

**For detailed manual setup instructions, see [SETUP.md](SETUP.md).**

The project is a standard Maven project. To run it from the command line,
type `mvnw` (Windows), or `./mvnw` (Mac & Linux), then open
http://localhost:8080 in your browser.

You can also import the project to your IDE of choice as you would with any
Maven project. Read more on [how to import Vaadin projects to different 
IDEs](https://vaadin.com/docs/latest/guide/step-by-step/importing) (Eclipse, IntelliJ IDEA, NetBeans, and VS Code).

## Deploying to Production

To create a production build, call `mvnw clean package -Pproduction` (Windows),
or `./mvnw clean package -Pproduction` (Mac & Linux).
This will build a JAR file with all the dependencies and front-end resources,
ready to be deployed. The file can be found in the `target` folder after the build completes.

Once the JAR file is built, you can run it using
`java -jar target/demoshactor-1.0-SNAPSHOT.jar`

## Testing

### Running Tests

The project includes comprehensive unit tests to ensure code quality and functionality. You can run tests using Maven:

#### Run All Tests
```bash
# Windows
mvnw test

# Mac & Linux
./mvnw test
```

#### Run Specific Test Classes
```bash
# Run a specific test class
mvnw test -Dtest=DynamicUITerminologyTest

# Run tests in a specific package
mvnw test -Dtest="shactor.utils.formatters.*"
```

#### Run Tests with Coverage
```bash
mvnw clean test jacoco:report
```





## Project structure

- `MainLayout.java` in `src/main/java` contains the navigation setup (i.e., the
  side/top bar and the main menu). This setup uses
  [App Layout](https://vaadin.com/docs/components/app-layout).
- `views` package in `src/main/java` contains the server-side Java views of your application.
- `views` folder in `frontend/` contains the client-side JavaScript views of your application.
- `themes` folder in `frontend/` contains the custom CSS styles.
- `src/test/java` contains unit tests organized by package structure.

### Useful links (In case you are not familiar with Vaadin framework)

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorials at [vaadin.com/tutorials](https://vaadin.com/tutorials).
- Watch training videos and get certified at [vaadin.com/learn/training](https://vaadin.com/learn/training).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/components](https://vaadin.com/components).
- View use case applications that demonstrate Vaadin capabilities at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Build any UI without custom CSS by discovering Vaadin's set of [CSS utility classes](https://vaadin.com/docs/styling/lumo/utility-classes). 
- Find a collection of solutions to common use cases at [cookbook.vaadin.com](https://cookbook.vaadin.com/).
- Find add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin).


### License 
[![CC BY-NC-ND 4.0][cc-by-nc-nd-shield]][cc-by-nc-nd]

This work is licensed under a
[Creative Commons Attribution-NonCommercial-NoDerivs 4.0 International License][cc-by-nc-nd].

[![CC BY-NC-ND 4.0][cc-by-nc-nd-image]][cc-by-nc-nd]

[cc-by-nc-nd]: https://creativecommons.org/licenses/by-nc-nd/4.0/
[cc-by-nc-nd-image]: https://licensebuttons.net/l/by-nc-nd/4.0/88x31.png
[cc-by-nc-nd-shield]: https://img.shields.io/badge/License-CC%20BY--NC--ND%204.0-lightgrey.svg
