/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.forms.modules.wizard.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.forms.business.Form;
import fr.paris.lutece.plugins.forms.business.FormHome;
import fr.paris.lutece.plugins.forms.business.FormMessage;
import fr.paris.lutece.plugins.forms.business.FormMessageHome;
import fr.paris.lutece.plugins.forms.business.Step;
import fr.paris.lutece.plugins.forms.business.StepHome;
import fr.paris.lutece.plugins.forms.business.Transition;
import fr.paris.lutece.plugins.forms.business.TransitionHome;
import fr.paris.lutece.plugins.forms.modules.wizard.business.FormWizardDraft;
import fr.paris.lutece.plugins.forms.modules.wizard.business.WizardStepDraft;
import fr.paris.lutece.plugins.forms.service.FormBuilderException;
import fr.paris.lutece.plugins.forms.service.FormBuilderService;
import fr.paris.lutece.plugins.forms.service.StepService;
import fr.paris.lutece.plugins.forms.service.cache.FormsCacheService;
import fr.paris.lutece.plugins.forms.service.json.IStepTemplateProvider;
import fr.paris.lutece.plugins.forms.service.json.StepTemplatePreview;
import fr.paris.lutece.api.user.User;
import fr.paris.lutece.portal.service.accesscontrol.AccessControlService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Creates a form, its steps and the transitions between them from the choices made in the wizard
 */
@ApplicationScoped
public class FormWizardService
{
    public static final String MESSAGE_ERROR_CREATION = "module.forms.wizard.message.error.creation";
    private static final String MESSAGE_ERROR_FIRST_STEP_TITLE = "module.forms.wizard.message.error.firstStepTitle";
    private static final String MESSAGE_ERROR_LAST_STEP_TITLE = "module.forms.wizard.message.error.lastStepTitle";
    private static final String MESSAGE_ERROR_INTERMEDIATE_STEP_TITLE = "module.forms.wizard.message.error.intermediateStepTitle";
    private static final String MESSAGE_ERROR_STEP_TITLE_SIZE = "module.forms.wizard.message.error.stepTitleSize";
    private static final String MESSAGE_ERROR_STEP_DESCRIPTION_SIZE = "module.forms.wizard.message.error.stepDescriptionSize";
    private static final String MESSAGE_ERROR_TEMPLATE_NOT_FOUND = "module.forms.wizard.message.error.templateNotFound";
    private static final String LABEL_FIRST_STEP = "module.forms.wizard.steps.sectionFirst";
    private static final String LABEL_SINGLE_STEP = "module.forms.wizard.steps.sectionSingle";
    private static final String LABEL_LAST_STEP = "module.forms.wizard.steps.sectionLast";
    private static final String LABEL_INTERMEDIATE_STEP = "module.forms.wizard.steps.labelIntermediate";
    private static final int MAX_SIZE = 255;

    @Inject
    private StepService _stepService;
    @Inject
    private FormBuilderService _formBuilderService;
    @Inject
    private FormsCacheService _formsCacheService;
    @Inject
    private AccessControlService _accessControlService;

    /**
     * The access controls the user can link to a form, as offered by the forms creation screen: the first item, -1, stands for none
     *
     * @param user
     *            the user creating the form
     * @param locale
     *            the locale of the labels
     * @return the access controls, empty when no access control service is installed
     */
    public ReferenceList getAccessControls( User user, Locale locale )
    {
        ReferenceList list = _accessControlService.isAvailable( ) ? _accessControlService.getAccessControlsEnabled( user, locale ) : null;
        return list != null ? list : new ReferenceList( );
    }

    /**
     * Tells whether an access control can be linked by the user: none (-1) always can, another one only if it is offered to the user
     *
     * @param nIdAccessControl
     *            the access control, -1 for none
     * @param user
     *            the user creating the form
     * @param locale
     *            the locale of the labels
     * @return true if the access control can be linked
     */
    public boolean isAccessControlOffered( int nIdAccessControl, User user, Locale locale )
    {
        if ( nIdAccessControl == -1 )
        {
            return true;
        }
        String strId = String.valueOf( nIdAccessControl );
        return getAccessControls( user, locale ).stream( ).anyMatch( item -> strId.equals( item.getCode( ) ) );
    }

    /**
     * Tells whether step templates can be offered, that is whether a step template provider (module forms-template) is installed
     *
     * @return true if step templates are available
     */
    public boolean isTemplateAvailable( )
    {
        return _stepService.getStepTemplateProvider( ) != null;
    }

    /**
     * The step templates offered for the intermediate steps
     *
     * @return the step templates, empty when no provider is installed
     */
    public ReferenceList getStepTemplates( )
    {
        IStepTemplateProvider provider = _stepService.getStepTemplateProvider( );
        if ( provider == null )
        {
            return new ReferenceList( );
        }
        ReferenceList list = provider.getStepTemplateList( );
        return list != null ? list : new ReferenceList( );
    }

    /**
     * The descriptions of the step templates, shown in the preview of the template chosen for a step
     *
     * @return the descriptions keyed by the code of the template, empty when no provider is installed
     */
    public Map<String, String> getStepTemplateDescriptions( )
    {
        IStepTemplateProvider provider = _stepService.getStepTemplateProvider( );
        Map<String, String> mapDescriptions = provider != null ? provider.getStepTemplateDescriptions( ) : null;
        return mapDescriptions != null ? mapDescriptions : Collections.emptyMap( );
    }

    /**
     * The previews of the step templates (a thumbnail and the fields of the step), shown for the template chosen for a step
     *
     * @return the previews keyed by the code of the template, empty when no provider is installed or when it draws none
     */
    public Map<String, StepTemplatePreview> getStepTemplatePreviews( )
    {
        IStepTemplateProvider provider = _stepService.getStepTemplateProvider( );
        Map<String, StepTemplatePreview> mapPreviews = provider != null ? provider.getStepTemplatePreviews( ) : null;
        return mapPreviews != null ? mapPreviews : Collections.emptyMap( );
    }

    /**
     * Checks the steps of the draft and fills the title of the templates chosen
     *
     * @param draft
     *            the wizard draft
     * @param locale
     *            the locale of the messages
     * @return the error messages, empty if the steps are valid
     */
    public List<String> validateSteps( FormWizardDraft draft, Locale locale )
    {
        List<String> listErrors = new ArrayList<>( );
        ReferenceList listTemplates = getStepTemplates( );
        String strFirstLabel = I18nService.getLocalizedString( draft.isMultiStep( ) ? LABEL_FIRST_STEP : LABEL_SINGLE_STEP, locale );
        validateStep( draft.getFirstStep( ), MESSAGE_ERROR_FIRST_STEP_TITLE, null, strFirstLabel, listTemplates, listErrors, locale );
        if ( draft.isMultiStep( ) )
        {
            int nIndex = 1;
            for ( WizardStepDraft step : draft.getIntermediateSteps( ) )
            {
                Object [ ] args = {
                        nIndex
                };
                String strLabel = I18nService.getLocalizedString( LABEL_INTERMEDIATE_STEP, locale ) + " " + nIndex;
                validateStep( step, MESSAGE_ERROR_INTERMEDIATE_STEP_TITLE, args, strLabel, listTemplates, listErrors, locale );
                nIndex++;
            }
            validateStep( draft.getLastStep( ), MESSAGE_ERROR_LAST_STEP_TITLE, null, I18nService.getLocalizedString( LABEL_LAST_STEP, locale ), listTemplates,
                    listErrors, locale );
        }
        return listErrors;
    }

    /**
     * Creates the form described by the draft: the form, its messages, its link to an access control, its steps and the transitions linking them in order.
     * Everything is created or nothing.
     *
     * @param draft
     *            the wizard draft, whose steps are valid
     * @param locale
     *            the locale, used to import the step templates
     * @return the identifier of the created form
     * @throws FormWizardException
     *             if a step template cannot be imported
     */
    @Transactional
    public int createForm( FormWizardDraft draft, Locale locale )
    {
        try
        {
            Form form = FormHome.create( draft.getForm( ) );
            int nIdForm = form.getId( );

            FormMessage formMessage = draft.getFormMessage( );
            formMessage.setIdForm( nIdForm );
            FormMessageHome.create( formMessage );

            if ( draft.getIdAccessControl( ) != -1 && _accessControlService.isAvailable( ) )
            {
                _accessControlService.linkResourceToAccessControl( nIdForm, Form.RESOURCE_TYPE, draft.getIdAccessControl( ) );
            }

            int nIdPreviousStep = addStep( nIdForm, draft.getFirstStep( ), 0, true, !draft.isMultiStep( ), locale );
            if ( draft.isMultiStep( ) )
            {
                for ( WizardStepDraft stepDraft : draft.getIntermediateSteps( ) )
                {
                    nIdPreviousStep = addStep( nIdForm, stepDraft, nIdPreviousStep, false, false, locale );
                }
                addStep( nIdForm, draft.getLastStep( ), nIdPreviousStep, false, true, locale );
            }
            return nIdForm;
        }
        catch( RuntimeException e )
        {
            // The rolled back rows may already be in the cache
            _formsCacheService.resetCache( );
            throw e;
        }
    }

    /**
     * Adds a step to the form, empty or imported from a step template, linked to the previous step by a transition
     *
     * @return the identifier of the added step
     */
    private int addStep( int nIdForm, WizardStepDraft stepDraft, int nIdPreviousStep, boolean bInitial, boolean bFinal, Locale locale )
    {
        if ( stepDraft.isFromTemplate( ) )
        {
            return importTemplateStep( nIdForm, stepDraft, nIdPreviousStep, bInitial, bFinal, locale );
        }
        Step step = createStep( nIdForm, stepDraft, bInitial, bFinal );
        if ( nIdPreviousStep > 0 )
        {
            createTransition( nIdPreviousStep, step.getId( ) );
        }
        return step.getId( );
    }

    /**
     * Imports a step template after the given step (none for the first step), which creates the transition. The imported step takes the place given by
     * the wizard (initial, final) and the name of the template, rather than the "copy of" title given by the forms import.
     */
    private int importTemplateStep( int nIdForm, WizardStepDraft stepDraft, int nIdPreviousStep, boolean bInitial, boolean bFinal, Locale locale )
    {
        int nIdStep;
        try
        {
            nIdStep = _formBuilderService.importStep( nIdForm, null, stepDraft.getIdTemplate( ), nIdPreviousStep, locale );
        }
        catch( FormBuilderException e )
        {
            throw new FormWizardException( MESSAGE_ERROR_CREATION, e );
        }
        Step step = StepHome.findByPrimaryKey( nIdStep );
        step.setInitial( bInitial );
        step.setFinal( bFinal );
        if ( StringUtils.isNotBlank( stepDraft.getTemplateTitle( ) ) )
        {
            step.setTitle( StringUtils.left( stepDraft.getTemplateTitle( ), MAX_SIZE ) );
        }
        StepHome.update( step );
        return nIdStep;
    }

    private static Step createStep( int nIdForm, WizardStepDraft stepDraft, boolean bInitial, boolean bFinal )
    {
        Step step = new Step( );
        step.setIdForm( nIdForm );
        step.setTitle( stepDraft.getTitle( ) );
        step.setDescription( StringUtils.defaultString( stepDraft.getDescription( ) ) );
        step.setInitial( bInitial );
        step.setFinal( bFinal );
        step.setTitleDisplayed( true );
        return StepHome.create( step );
    }

    private static void createTransition( int nIdFromStep, int nIdNextStep )
    {
        Transition transition = new Transition( );
        transition.setFromStep( nIdFromStep );
        transition.setNextStep( nIdNextStep );
        TransitionHome.create( transition );
    }

    /**
     * Checks a step: a template step needs a template still offered, an empty step a title
     */
    private static void validateStep( WizardStepDraft step, String strMandatoryKey, Object [ ] args, String strLabel, ReferenceList listTemplates,
            List<String> listErrors, Locale locale )
    {
        if ( step.isFromTemplate( ) )
        {
            String strTemplateTitle = findTemplateTitle( listTemplates, step.getIdTemplate( ) );
            if ( strTemplateTitle == null )
            {
                listErrors.add( I18nService.getLocalizedString( MESSAGE_ERROR_TEMPLATE_NOT_FOUND, new Object [ ] {
                        strLabel
                }, locale ) );
            }
            step.setTemplateTitle( strTemplateTitle );
            return;
        }
        if ( StringUtils.isBlank( step.getTitle( ) ) )
        {
            listErrors.add( args == null ? I18nService.getLocalizedString( strMandatoryKey, locale )
                    : I18nService.getLocalizedString( strMandatoryKey, args, locale ) );
        }
        else
            if ( step.getTitle( ).length( ) > MAX_SIZE )
            {
                listErrors.add( I18nService.getLocalizedString( MESSAGE_ERROR_STEP_TITLE_SIZE, locale ) );
            }
        if ( StringUtils.length( step.getDescription( ) ) > MAX_SIZE )
        {
            listErrors.add( I18nService.getLocalizedString( MESSAGE_ERROR_STEP_DESCRIPTION_SIZE, locale ) );
        }
    }

    private static String findTemplateTitle( ReferenceList listTemplates, int nIdTemplate )
    {
        String strId = String.valueOf( nIdTemplate );
        return listTemplates.stream( ).filter( item -> strId.equals( item.getCode( ) ) ).map( ReferenceItem::getName ).findFirst( ).orElse( null );
    }
}
