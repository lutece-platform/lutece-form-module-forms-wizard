# Module Forms Wizard

## Introduction

This module of [plugin-forms](https://github.com/lutece-platform/lutece-form-plugin-forms) adds a guided wizard to
create a form in the back office.

Without it, creating a usable form takes several separate screens: create the form, open its steps, create a first
step and a last step, link them with transitions, then open the builder. The wizard groups these operations in one
flow of four screens and leaves the administrator in the forms builder, ready to add the questions.

## Features

* **Form information**: title, breadcrumb, description and end message of the form.
* **Structure**: a single-step form, or a multiple-steps form.
* **Steps**: the first step, the last step and any number of intermediate steps, which can be reordered or removed.
  An intermediate step starts empty or from a **step template** when a template provider is installed
  (for example module-forms-template).
* **Summary**: a read-only recap with links back to each screen, then the creation of the form.

The steps are chained by linear transitions: first step → intermediate steps → last step. Conditional transitions
and controls are still configured in the forms screens.

The draft of the wizard is kept in the administrator's session. Nothing is written to the database before the
confirmation, and the form, its steps, its transitions and the imported templates are then created in a single
transaction: a failure leaves no half-built form. The module has no database table of its own.

## Installation

Add the module to the `pom.xml` of the site, next to plugin-forms:

```xml
<dependency>
    <groupId>fr.paris.lutece.plugins</groupId>
    <artifactId>module-forms-wizard</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <type>lutece-plugin</type>
</dependency>
```

Then enable the `forms-wizard` plugin in the back office (**System > Plugins management**).

### Requirements

* Lutece core 8
* plugin-forms 4.1.0 or later, which provides the `IFormCreationActionProvider` extension point used to show the
  wizard button
* Optional: a step template provider (`IStepTemplateProvider`), such as module-forms-template, to build
  intermediate steps from templates. Without one, the template choice is simply not shown.

## Usage

On the forms management page, click **Create with the wizard**, next to the **Create** button. The button is shown
to administrators who have the `FORMS_MANAGEMENT` right and the RBAC permission to create forms: the wizard reuses
the forms rights and declares none of its own.

Fill in the four screens, then click **Create the form**: the forms builder opens on the new form.

## Build

```bash
mvn clean install
```

## Links

* Source code: <https://github.com/lutece-platform/lutece-form-module-forms-wizard>
* Lutece: <https://lutece.paris.fr>
