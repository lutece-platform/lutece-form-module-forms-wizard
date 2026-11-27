# Design: Forms creation wizard

**Date:** 2026-11-10
**Status:** Approved, implemented (see Implementation notes)
**Module:** lutece-form-module-forms-wizard (`fr.paris.lutece.plugins:module-forms-wizard`)
**Depends on:** plugin-forms 4.0.2-SNAPSHOT+ (and, at runtime only, module-forms-template if installed)
**Lutece version:** 8.x

## Context

Creating a form in plugin-forms means a number of separate screens: create the form, open the steps screen,
create an initial step, create a final step, link them with transitions, then go to the builder. New admins
get lost. The wizard simplifies this into one guided flow and leaves the user in the builder, ready to add fields.

## Decision

Approach A: a **session-held wizard draft**, saved in **one transaction** at the end.

- Nothing is written to the database before the user confirms, so a stopped wizard leaves no half-built forms.
- Back/next navigation is free.
- The module has no tables of its own.

Step templates come from the **existing plugin-forms extension point** `IStepTemplateProvider`
(`StepService.getStepTemplateProvider()`), which module-forms-template implements. The wizard has **no Maven
dependency** on forms-template. The template choice is shown only when a provider is present.

## User flow

```
Forms list ──[Create with the wizard]──► FormWizardJspBean
  ① Form info      title*, breadcrumb name*, description, end message
  ② Structure      ( ) Single step   ( ) Multiple steps
  ③a Single        step title*, description            → step initial=true, final=true
  ③b Multiple      First step* (title*, description)   → initial=true
                   Intermediate steps (0..n), each:
                       ( ) empty    → title*, description
                       ( ) template → choice from IStepTemplateProvider (hidden if no provider)
                       reorder ↑↓ / remove
                   Last step* (title*, description)    → final=true
  ④ Summary        read-only recap, links back to each screen → [Create]
        │
        ▼ FormWizardService.createForm(draft)   @Transactional
        ▼ redirect jsp/admin/plugins/forms/ManageFormBuilder.jsp?view=builder&id_form=N
```

- Templates are offered for **intermediate steps only**. The first and last steps are always built from title + description.
- Transitions are **linear**: first → i1 → … → in → last. Conditional transitions and controls stay in the forms screens.

## Design

### Business (session state, not persisted)

`business/FormWizardDraft`, `@SessionScoped @Named`
- `Form form`: title (`@NotEmpty`, `@Size 255`), breadcrumbName (`@NotEmpty`), description (`@Size 255`).
  The constraints of `Form` are reused through `validateBean`.
- `FormMessage formMessage`: endMessage (`@Size 3000`), endMessageDisplay = true when filled.
- `boolean multiStep`
- `WizardStepDraft firstStep`, `WizardStepDraft lastStep` (single mode uses `firstStep` only)
- `List<WizardStepDraft> intermediateSteps`
- `int currentScreen` (furthest screen reached, so the stepper links only to screens already completed)
- `reset()`

`business/WizardStepDraft` (Serializable)
- `String title`, `String description`, `int idTemplate` (0 = empty step), `String templateTitle` (for the summary)
- Validation: title required and ≤ 255 when `idTemplate == 0`; template must exist in the provider list otherwise.

### Service

`service/FormWizardService`, `@ApplicationScoped`
- `boolean isTemplateAvailable()`: `StepService.getStepTemplateProvider() != null`
- `ReferenceList getStepTemplates()`: `provider.getStepTemplateList()` or empty
- `@Transactional int createForm( FormWizardDraft draft, AdminUser user, Locale locale )`
  1. `FormHome.create(form)` (creation date set, like in `FormJspBean.doCreateForm`); `FormMessageHome.create`
  2. Access control link when `AccessControlService.isAvailable()` (default: none)
  3. First step: `StepHome.create` with initial=true and final=!multiStep
  4. For each intermediate:
     - empty → `StepHome.create`, then `TransitionHome.create(previous → step)`
     - template → `FormBuilderService.importStep( idForm, null, idTemplate, idPrevious, locale )`
       (imports the template's questions/groups and creates the transition from `idPrevious`)
  5. Last step: `StepHome.create` with final=true, then the transition from the previous step
  6. Reset the forms cache and remove backups, the same as the forms JspBeans
  7. Returns the form id
  The whole method rolls back on any error, so no orphan form is left.

`service/WizardFormCreationActionProvider`, `@ApplicationScoped`, implements the new
`fr.paris.lutece.plugins.forms.service.IFormCreationActionProvider` (see plugin-forms change).

### Web

`web/FormWizardJspBean`, `@SessionScoped @Named`,
`@Controller( controllerJsp = "ManageFormWizard.jsp", controllerPath = "jsp/admin/plugins/forms/modules/wizard/", right = "FORMS_MANAGEMENT", securityTokenEnabled = true )`

| Kind | Name | Behaviour |
|---|---|---|
| action | `start` | reset the draft, redirect to `formInfo` |
| view (default) | `formInfo` | screen ① |
| action | `saveFormInfo` | populate + validate Form/FormMessage → `structure` |
| view | `structure` | screen ② |
| action | `saveStructure` | set multiStep → `steps` |
| view | `steps` | screen ③a or ③b (template list in the model when available) |
| action | `addIntermediate` / `removeIntermediate` / `moveIntermediate` | edit the list, redirect back to `steps` (values typed so far are posted and kept) |
| action | `saveSteps` | validate (first/last required in multi mode) → `summary` |
| view | `summary` | screen ④ |
| action | `create` | `FormWizardService.createForm`, `draft.reset()`, redirect to the forms builder |

Every view and action checks RBAC `Form.RESOURCE_TYPE / * / FormsResourceIdService.PERMISSION_CREATE`, the same as
"Create form". A view reached before its previous screen is valid redirects to the first screen that is not valid yet.

JSP: `webapp/jsp/admin/plugins/forms/modules/wizard/ManageFormWizard.jsp` (the same pattern as `ManageForms.jsp`).

### Templates (BO macros, Tabler)

`webapp/WEB-INF/templates/admin/plugins/forms/modules/wizard/`
- `wizard_stepper.html`: shared header ①–④ (included by each screen)
- `wizard_form_info.html`, `wizard_structure.html`, `wizard_steps.html`, `wizard_summary.html`

Follow `.claude/rules/template-back-office.md` and the `lutece-update-template-bo` rules: page hierarchy,
no inline form, no offcanvas, and no `<#if>` inside macro attribute strings.

### Plugin descriptor

`webapp/WEB-INF/plugins/forms-wizard.xml`: name `forms-wizard`, no admin feature (the forms right `FORMS_MANAGEMENT`
is reused), `min-core-version 8.0.0`, icon taken from forms. `src/main/resources/META-INF/beans.xml`.
SQL: an empty `init_core_forms-wizard.sql` only, if the build requires it.

### i18n

`src/java/fr/paris/lutece/plugins/forms/modules/wizard/resources/wizard_messages.properties` and `_fr`:
module description/provider, the "Create with the wizard" button, the titles of the 4 screens and the stepper labels,
the field labels, the single/multiple choice, empty/template choice, the summary, and the errors
(`error.firstStepRequired`, `error.lastStepRequired`, `error.templateNotFound`, `error.creation`), plus the info message `info.formCreated`.

### plugin-forms change (separate LUT ticket / PR)

- New `service/IFormCreationActionProvider`: `String getTitleKey()`, `String getIcon()`, `String getUrl()`,
  `boolean isAuthorized( AdminUser user )`.
- `FormJspBean.getManageForms`: `Instance<IFormCreationActionProvider>` puts the authorized ones in the model as
  `form_creation_actions`.
- `manage_forms.html`: render each action as an `@aButton` next to "Create" (line ~17) and as an extra
  action in the `@empty` state (line ~137).
- Backward compatible: with no provider, nothing changes.

## Testing

- `FormWizardServiceTest` (library-lutece-unit-testing, DB):
  1. single step → 1 step, initial & final, 0 transitions, FormMessage saved
  2. multiple steps with 2 empty intermediates → 4 steps, 3 transitions in order, exactly one initial and one final
  3. template intermediate through a stub `IStepTemplateProvider` → the template's questions are imported, the chain is intact
  4. forced failure on the last step → rollback, no form left
- `FormWizardDraft` validation unit tests (empty titles, template without id, multi mode without last step).
- e2e (`lpe2e`): the wizard screens, then the YAML scenario "create 3-step form with template" lands on the builder.

## Files to create

- [ ] `pom.xml` (parent lutece-global-pom 8.x, deps lutece-core, plugin-forms, library-lutece-unit-testing)
- [ ] `webapp/WEB-INF/plugins/forms-wizard.xml`
- [ ] `src/main/resources/META-INF/beans.xml`
- [ ] `src/java/.../wizard/business/FormWizardDraft.java`, `WizardStepDraft.java`
- [ ] `src/java/.../wizard/service/FormWizardService.java`, `WizardFormCreationActionProvider.java`
- [ ] `src/java/.../wizard/web/FormWizardJspBean.java`
- [ ] `src/java/.../wizard/resources/wizard_messages.properties`, `wizard_messages_fr.properties`
- [ ] `webapp/jsp/admin/plugins/forms/modules/wizard/ManageFormWizard.jsp`
- [ ] `webapp/WEB-INF/templates/admin/plugins/forms/modules/wizard/*.html` (5 files)
- [ ] `src/test/java/.../wizard/service/FormWizardServiceTest.java` + draft validation test
- [ ] `README.md`, `src/site/xdoc/index.xml`
- [ ] plugin-forms: `IFormCreationActionProvider.java`, `FormJspBean.java`, `manage_forms.html`, forms test

## Implementation order

1. plugin-forms extension point (small, separate PR), then build and install the SNAPSHOT
2. Module skeleton (pom, descriptor, beans.xml, i18n) that compiles and loads in the loose app
3. Draft + `FormWizardService` + service tests
4. JspBean + JSP + templates, one screen at a time, checked with curl on localhost:9080
5. Provider → button on the forms list
6. e2e run, then `lutece-v8-review`

## Out of scope

- Conditional transitions, controls, branching between steps
- Templates for the first/last steps
- Workflow, category, availability dates, captcha, authentication, and other form parameters (set later in the forms screens)
- Saving the draft in the database / resuming a wizard in another session
- Front-office changes

## Implementation notes (2026-11-10)

- **Draft holder:** `FormWizardDraft` is a plain `Serializable` object held by the `@SessionScoped` `FormWizardJspBean`
  (the way `FormJspBean` holds `_form`), not a separate CDI bean. Same lifetime, and it is simpler to test.
- **Template step title:** forms renames an imported step to "Copy of …". The wizard gives the imported step the
  name of the template instead, and it is never initial nor final.
- **UI:** each screen follows the Tabler wizard (preview.tabler.io/wizard.html): a centred `container-narrow`,
  a `card card-md` with an icon, a title and an introduction, an `hr-text` section label "Step n of 4", the fields,
  then under the card a progress bar and a `btn-list` (Previous as a link, Continue/Create as the primary button).
  The summary lists the steps with Tabler `steps steps-vertical` and the form data in a `datagrid`.
  Shared frame: `wizard_commons.html` (`@wizardScreen`).
- **Buttons:** every screen posts one action. The `operation` parameter (`next`, `previous`, `add`, `remove_i`,
  `up_i`, `down_i`) tells the buttons apart, so there is one CSRF token per view. A hidden first submit makes Enter mean "Continue".
- **Breadcrumb:** "breadcrumb name" is the breadcrumb type select of forms (`BreadcrumbManager`), defaulted to the first type.
- **Workgroup:** the user's only workgroup, otherwise "all", the same as the forms creation screen.
