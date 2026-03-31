# ADC-PEI 25/26 — First Web Application

A simple Java web application built with Jersey (JAX-RS) and deployed on Google App Engine. It exposes two REST endpoints and serves a basic HTML front page.

---

## What this app does

Once running, the app serves a simple web page with two available services:

- **`/rest/utils/hello`** — returns a plain text greeting
- **`/rest/utils/time`** — returns the current server time in JSON

---

## Prerequisites

Before you begin, make sure you have the following installed:

- **Google Cloud Account**
  > ⚠️ See the instructions to redeem your cupon in shared materials: **`ADC_Project_Google_Cloud_Account_Creation.pdf`**
- [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
- [Python 3.10](https://www.python.org/downloads/release/python-3100/) (for the Google Cloud SDK download)
- [Apache Maven](https://maven.apache.org/install.html)
- [Git](https://git-scm.com/)
- [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) (for cloud deployment)
- [Eclipse IDE](https://www.eclipse.org/downloads/) with the Maven plugin

---

## Getting Started

### 1. Fork and clone the repository

Fork the project on GitHub, then clone your fork locally:

```bash
git clone git@github.com:ADC-Projeto/adc-pei-2526-part1.git
cd adc-pei-2526-part1
```

### 2. Import into Eclipse

1. Open Eclipse and go to **File → Import → Maven → Existing Maven Projects**
2. Navigate to the folder where you cloned the project
3. Select it and click **Finish**
4. Eclipse will resolve dependencies automatically — check for any errors in the **Problems** tab

---

## Building the project

From the project root, run:

```bash
mvn clean package
```

If the build succeeds, you'll find the compiled `.war` file at:

```
target/Firstwebapp-0.0.1.war
```

---

## Running locally

Start the local App Engine dev server with:

```bash
mvn appengine:run
```

Then open your browser and go to:

```
http://localhost:8080/
```

You should see the welcome page with links to the two available services. You can also test the endpoints directly:

```
http://localhost:8080/rest/utils/hello
http://localhost:8080/rest/utils/time
```

---

## Deploying to Google App Engine

### 1. Create a project on Google Cloud Console
Go to https://console.cloud.google.com/ and create a new project. Take note of the Project ID (e.g. `my-adc-app`).

### 2. Authenticate with Google Cloud

```bash
gcloud auth login
gcloud config set project <your-proj-id>
```

### 3. Deploy

```bash
mvn appengine:deploy -Dapp.deploy.projectId=<your-proj-id> -Dapp.deploy.version=<version-number>

```

After a successful deploy, your app will be live at:

```
https://<your-project-id>.appspot.com/
```

---

## Project Structure

```
src/
└── main/
    ├── java/
    │   └── pt/unl/fct/di/adc/firstwebapp/resources/
    │       └── ComputationResource.java   ← REST endpoints
    └── webapp/
        ├── index.html                     ← Front page
        └── WEB-INF/
            ├── web.xml                    ← Servlet config
            └── appengine-web.xml          ← App Engine config
```

---

## License

See [LICENSE](LICENSE) for details.

---

*FCT NOVA — ADC-PEI 2025/2026*
