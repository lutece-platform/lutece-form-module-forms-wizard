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
package fr.paris.lutece.plugins.forms.modules.wizard.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.forms.business.Form;
import fr.paris.lutece.plugins.forms.business.FormMessage;
import fr.paris.lutece.plugins.forms.modules.wizard.business.FormWizardDraft;
import fr.paris.lutece.plugins.forms.modules.wizard.business.WizardStepDraft;
import fr.paris.lutece.plugins.forms.modules.wizard.service.FormWizardException;
import fr.paris.lutece.plugins.forms.modules.wizard.service.FormWizardService;
import fr.paris.lutece.plugins.forms.service.FormsResourceIdService;
import fr.paris.lutece.plugins.forms.web.breadcrumb.BreadcrumbManager;
import fr.paris.lutece.plugins.forms.util.FormsConstants;
import fr.paris.lutece.portal.business.rbac.RBAC;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.url.UrlItem;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The form creation wizard: form information, single or multiple steps, the steps, then a summary before the creation
 */
@SessionScoped
@Named
@Controller( controllerJsp = "ManageFormWizard.jsp", controllerPath = "jsp/admin/plugins/forms/modules/wizard/", right = "FORMS_MANAGEMENT", securityTokenEnabled = true )
public class FormWizardJspBean extends MVCAdminJspBean
{
    private static final long serialVersionUID = 1L;

    // Views and actions: each screen posts to one action, the buttons of a screen being told apart by the operation parameter
    private static final String VIEW_FORM_INFO = "formInfo";
    private static final String ACTION_SAVE_FORM_INFO = "saveFormInfo";
    private static final String VIEW_STRUCTURE = "structure";
    private static final String ACTION_SAVE_STRUCTURE = "saveStructure";
    private static final String VIEW_STEPS = "steps";
    private static final String ACTION_SAVE_STEPS = "saveSteps";
    private static final String VIEW_SUMMARY = "summary";
    private static final String ACTION_CREATE_FORM = "createForm";

    // Templates
    private static final String TEMPLATE_FORM_INFO = "/admin/plugins/forms/modules/wizard/wizard_form_info.html";
    private static final String TEMPLATE_STRUCTURE = "/admin/plugins/forms/modules/wizard/wizard_structure.html";
    private static final String TEMPLATE_STEPS = "/admin/plugins/forms/modules/wizard/wizard_steps.html";
    private static final String TEMPLATE_SUMMARY = "/admin/plugins/forms/modules/wizard/wizard_summary.html";

    // Properties
    private static final String PROPERTY_PAGE_TITLE = "module.forms.wizard.pageTitle";

    // Messages
    private static final String MESSAGE_ERROR_STRUCTURE = "module.forms.wizard.message.error.structure";
    private static final String MESSAGE_ERROR_DATE_START_AFTER_END = "forms.error.form.date.startAfterEnd";
    private static final String MESSAGE_INFO_FORM_CREATED = "module.forms.wizard.message.info.formCreated";
    private static final String MESSAGE_ERROR_ACCESS = "module.forms.wizard.message.error.access";

    // Parameters
    private static final String PARAMETER_INIT = "init";
    private static final String PARAMETER_OPERATION = "operation";
    private static final String PARAMETER_TITLE = "title";
    private static final String PARAMETER_DESCRIPTION = "description";
    private static final String PARAMETER_BREADCRUMB_NAME = "breadcrumbName";
    private static final String PARAMETER_END_MESSAGE_DISPLAY = "endMessageDisplay";
    private static final String PARAMETER_END_MESSAGE = "endMessage";
    private static final String PARAMETER_WORKGROUP = "workgroup";
    private static final String PARAMETER_ID_ACCESS_CONTROL = "id_accesscontrol";
    private static final String PARAMETER_MULTI_STEP = "multi_step";
    private static final String PARAMETER_INTERMEDIATE_COUNT = "intermediate_count";
    // The fields of a step are named <prefix>_type, <prefix>_template, <prefix>_title and <prefix>_description
    private static final String PREFIX_FIRST_STEP = "first";
    private static final String PREFIX_LAST_STEP = "last";
    private static final String PREFIX_INTERMEDIATE_STEP = "intermediate_";
    private static final String PARAMETER_SUFFIX_TYPE = "_type";
    private static final String PARAMETER_SUFFIX_TEMPLATE = "_template";
    private static final String PARAMETER_SUFFIX_TITLE = "_title";
    private static final String PARAMETER_SUFFIX_DESCRIPTION = "_description";

    // Operations
    private static final String OPERATION_PREVIOUS = "previous";
    private static final String OPERATION_ADD = "add";
    private static final String OPERATION_REMOVE = "remove_";
    private static final String OPERATION_UP = "up_";
    private static final String OPERATION_DOWN = "down_";
    private static final String TYPE_TEMPLATE = "template";

    // Marks
    private static final String MARK_DRAFT = "draft";
    private static final String MARK_FORM = "form";
    private static final String MARK_FORM_MESSAGE = "formMessage";
    private static final String MARK_BREADCRUMB_LIST = "breadcrumb_list";
    private static final String MARK_WORKGROUP_LIST = "workgroup_list";
    private static final String MARK_ACCESS_CONTROL_LIST = "accesscontrol_list";
    private static final String MARK_TEMPLATE_AVAILABLE = "template_available";
    private static final String MARK_TEMPLATE_LIST = "template_list";
    private static final String MARK_TEMPLATE_DESCRIPTIONS = "template_descriptions";
    private static final String MARK_TEMPLATE_PREVIEWS = "template_previews";
    private static final String MARK_WIZARD_STEP = "wizard_step";
    private static final String MARK_WEBAPP_URL = "webapp_url";
    private static final String MARK_LOCALE = "locale";

    private static final String VALIDATION_FORM_ATTRIBUTES_PREFIX = "forms.model.entity.form.attribute.";
    private static final int MAX_INTERMEDIATE_STEPS = 50;
    private static final String URL_FORM_BUILDER = "jsp/admin/plugins/forms/ManageFormBuilder.jsp?view=builder";

    @Inject
    private FormWizardService _formWizardService;

    private FormWizardDraft _draft;

    /**
     * Screen 1: the form information
     *
     * @param request
     *            the request
     * @param model
     *            the model
     * @return the page
     * @throws AccessDeniedException
     *             if the user cannot create a form
     */
    @View( value = VIEW_FORM_INFO, defaultView = true, securityTokenAction = ACTION_SAVE_FORM_INFO )
    public String getFormInfo( HttpServletRequest request, Models model ) throws AccessDeniedException
    {
        checkCreatePermission( );
        if ( _draft == null || Boolean.parseBoolean( request.getParameter( PARAMETER_INIT ) ) )
        {
            _draft = newDraft( );
        }
        model.put( MARK_FORM, _draft.getForm( ) );
        model.put( MARK_FORM_MESSAGE, _draft.getFormMessage( ) );
        model.put( MARK_BREADCRUMB_LIST, BreadcrumbManager.getRefListBreadcrumb( ) );
        putAccessLists( model );
        model.put( MARK_WEBAPP_URL, AppPathService.getBaseUrl( request ) );
        model.put( MARK_LOCALE, getLocale( ).getLanguage( ) );
        return getWizardPage( model, 1, TEMPLATE_FORM_INFO );
    }

    /**
     * Saves the form information
     *
     * @param request
     *            the request
     * @return the next screen, or the same one with the errors
     * @throws AccessDeniedException
     *             if the user cannot create a form
     */
    @Action( ACTION_SAVE_FORM_INFO )
    public String doSaveFormInfo( HttpServletRequest request ) throws AccessDeniedException
    {
        checkCreatePermission( );
        if ( _draft == null )
        {
            _draft = newDraft( );
        }
        Form form = _draft.getForm( );
        form.setTitle( StringUtils.trimToEmpty( request.getParameter( PARAMETER_TITLE ) ) );
        form.setDescription( StringUtils.trimToEmpty( request.getParameter( PARAMETER_DESCRIPTION ) ) );
        String strBreadcrumb = request.getParameter( PARAMETER_BREADCRUMB_NAME );
        if ( isKnownBreadcrumb( strBreadcrumb ) )
        {
            form.setBreadcrumbName( strBreadcrumb );
        }
        String strWorkgroup = request.getParameter( PARAMETER_WORKGROUP );
        if ( isUserWorkgroup( strWorkgroup ) )
        {
            form.setWorkgroup( strWorkgroup );
        }
        int nIdAccessControl = NumberUtils.toInt( request.getParameter( PARAMETER_ID_ACCESS_CONTROL ), -1 );
        if ( _formWizardService.isAccessControlOffered( nIdAccessControl, getUser( ), getLocale( ) ) )
        {
            _draft.setIdAccessControl( nIdAccessControl );
        }
        // The availability dates are read by the bean binding, as in the forms screens
        Form formPosted = new Form( );
        populate( formPosted, request, getLocale( ) );
        form.setAvailabilityStartDate( formPosted.getAvailabilityStartDate( ) );
        form.setAvailabilityEndDate( formPosted.getAvailabilityEndDate( ) );
        FormMessage formMessage = _draft.getFormMessage( );
        formMessage.setEndMessageDisplay( request.getParameter( PARAMETER_END_MESSAGE_DISPLAY ) != null );
        formMessage.setEndMessage( StringUtils.defaultString( request.getParameter( PARAMETER_END_MESSAGE ) ) );

        boolean bValid = validateBean( form, VALIDATION_FORM_ATTRIBUTES_PREFIX );
        bValid = validateBean( formMessage, VALIDATION_FORM_ATTRIBUTES_PREFIX ) && bValid;
        if ( form.getAvailabilityStartDate( ) != null && form.getAvailabilityEndDate( ) != null
                && form.getAvailabilityStartDate( ).after( form.getAvailabilityEndDate( ) ) )
        {
            addError( MESSAGE_ERROR_DATE_START_AFTER_END, getLocale( ) );
            bValid = false;
        }
        _draft.setFormInfoDone( bValid );
        return redirectView( request, bValid ? VIEW_STRUCTURE : VIEW_FORM_INFO );
    }

    /**
     * Screen 2: a single step or a multiple steps form
     *
     * @param request
     *            the request
     * @param model
     *            the model
     * @return the page
     * @throws AccessDeniedException
     *             if the user cannot create a form
     */
    @View( value = VIEW_STRUCTURE, securityTokenAction = ACTION_SAVE_STRUCTURE )
    public String getStructure( HttpServletRequest request, Models model ) throws AccessDeniedException
    {
        checkCreatePermission( );
        String strMissingView = getMissingView( VIEW_STRUCTURE );
        if ( strMissingView != null )
        {
            return redirectView( request, strMissingView );
        }
        return getWizardPage( model, 2, TEMPLATE_STRUCTURE );
    }

    /**
     * Saves the structure of the form
     *
     * @param request
     *            the request
     * @return the next or the previous screen
     * @throws AccessDeniedException
     *             if the user cannot create a form
     */
    @Action( ACTION_SAVE_STRUCTURE )
    public String doSaveStructure( HttpServletRequest request ) throws AccessDeniedException
    {
        checkCreatePermission( );
        if ( _draft == null )
        {
            return redirectView( request, VIEW_FORM_INFO );
        }
        if ( OPERATION_PREVIOUS.equals( request.getParameter( PARAMETER_OPERATION ) ) )
        {
            return redirectView( request, VIEW_FORM_INFO );
        }
        String strMultiStep = request.getParameter( PARAMETER_MULTI_STEP );
        if ( !Boolean.TRUE.toString( ).equals( strMultiStep ) && !Boolean.FALSE.toString( ).equals( strMultiStep ) )
        {
            addError( MESSAGE_ERROR_STRUCTURE, getLocale( ) );
            return redirectView( request, VIEW_STRUCTURE );
        }
        boolean bMultiStep = Boolean.parseBoolean( strMultiStep );
        if ( bMultiStep != _draft.isMultiStep( ) )
        {
            _draft.setStepsDone( false );
        }
        _draft.setMultiStep( bMultiStep );
        _draft.setStructureDone( true );
        return redirectView( request, VIEW_STEPS );
    }

    /**
     * Screen 3: the steps of the form
     *
     * @param request
     *            the request
     * @param model
     *            the model
     * @return the page
     * @throws AccessDeniedException
     *             if the user cannot create a form
     */
    @View( value = VIEW_STEPS, securityTokenAction = ACTION_SAVE_STEPS )
    public String getSteps( HttpServletRequest request, Models model ) throws AccessDeniedException
    {
        checkCreatePermission( );
        String strMissingView = getMissingView( VIEW_STEPS );
        if ( strMissingView != null )
        {
            return redirectView( request, strMissingView );
        }
        boolean bTemplateAvailable = _formWizardService.isTemplateAvailable( );
        model.put( MARK_TEMPLATE_AVAILABLE, bTemplateAvailable );
        model.put( MARK_TEMPLATE_LIST, bTemplateAvailable ? _formWizardService.getStepTemplates( ) : new ReferenceList( ) );
        model.put( MARK_TEMPLATE_DESCRIPTIONS, bTemplateAvailable ? _formWizardService.getStepTemplateDescriptions( ) : Collections.emptyMap( ) );
        model.put( MARK_TEMPLATE_PREVIEWS, bTemplateAvailable ? _formWizardService.getStepTemplatePreviews( ) : Collections.emptyMap( ) );
        return getWizardPage( model, 3, TEMPLATE_STEPS );
    }

    /**
     * Saves the steps, and adds, removes or moves an intermediate step
     *
     * @param request
     *            the request
     * @return the steps screen after an edit of the list, the summary when the steps are valid, or the previous screen
     * @throws AccessDeniedException
     *             if the user cannot create a form
     */
    @Action( ACTION_SAVE_STEPS )
    public String doSaveSteps( HttpServletRequest request ) throws AccessDeniedException
    {
        checkCreatePermission( );
        if ( _draft == null || !_draft.isStructureDone( ) )
        {
            return redirectView( request, VIEW_FORM_INFO );
        }
        readSteps( request );
        _draft.setStepsDone( false );

        String strOperation = StringUtils.defaultString( request.getParameter( PARAMETER_OPERATION ) );
        if ( OPERATION_PREVIOUS.equals( strOperation ) )
        {
            return redirectView( request, VIEW_STRUCTURE );
        }
        if ( _draft.isMultiStep( ) && editIntermediateSteps( strOperation ) )
        {
            return redirectView( request, VIEW_STEPS );
        }

        List<String> listErrors = _formWizardService.validateSteps( _draft, getLocale( ) );
        if ( !listErrors.isEmpty( ) )
        {
            listErrors.forEach( this::addError );
            return redirectView( request, VIEW_STEPS );
        }
        _draft.setStepsDone( true );
        return redirectView( request, VIEW_SUMMARY );
    }

    /**
     * Screen 4: the summary before the creation
     *
     * @param request
     *            the request
     * @param model
     *            the model
     * @return the page
     * @throws AccessDeniedException
     *             if the user cannot create a form
     */
    @View( value = VIEW_SUMMARY, securityTokenAction = ACTION_CREATE_FORM )
    public String getSummary( HttpServletRequest request, Models model ) throws AccessDeniedException
    {
        checkCreatePermission( );
        String strMissingView = getMissingView( VIEW_SUMMARY );
        if ( strMissingView != null )
        {
            return redirectView( request, strMissingView );
        }
        model.put( MARK_BREADCRUMB_LIST, BreadcrumbManager.getRefListBreadcrumb( ) );
        putAccessLists( model );
        return getWizardPage( model, 4, TEMPLATE_SUMMARY );
    }

    /**
     * Creates the form, then opens it in the forms builder
     *
     * @param request
     *            the request
     * @return the forms builder of the new form, or the summary with an error
     * @throws AccessDeniedException
     *             if the user cannot create a form
     */
    @Action( ACTION_CREATE_FORM )
    public String doCreateForm( HttpServletRequest request ) throws AccessDeniedException
    {
        checkCreatePermission( );
        if ( OPERATION_PREVIOUS.equals( request.getParameter( PARAMETER_OPERATION ) ) )
        {
            return redirectView( request, VIEW_STEPS );
        }
        String strMissingView = getMissingView( VIEW_SUMMARY );
        if ( strMissingView != null )
        {
            return redirectView( request, strMissingView );
        }
        if ( !isUserWorkgroup( _draft.getForm( ).getWorkgroup( ) )
                || !_formWizardService.isAccessControlOffered( _draft.getIdAccessControl( ), getUser( ), getLocale( ) ) )
        {
            addError( MESSAGE_ERROR_ACCESS, getLocale( ) );
            _draft.setFormInfoDone( false );
            return redirectView( request, VIEW_FORM_INFO );
        }
        // The templates may have changed since the steps screen
        List<String> listErrors = _formWizardService.validateSteps( _draft, getLocale( ) );
        if ( !listErrors.isEmpty( ) )
        {
            listErrors.forEach( this::addError );
            _draft.setStepsDone( false );
            return redirectView( request, VIEW_STEPS );
        }

        int nIdForm;
        try
        {
            nIdForm = _formWizardService.createForm( _draft, getLocale( ) );
        }
        catch( FormWizardException e )
        {
            AppLogService.error( "Form wizard: the form could not be created", e );
            addError( e.getMessage( ), getLocale( ) );
            return redirectView( request, VIEW_SUMMARY );
        }
        _draft = null;
        addInfo( MESSAGE_INFO_FORM_CREATED, getLocale( ) );

        // Absolute: a relative URL would resolve against the folder of the wizard JSP
        UrlItem url = new UrlItem( AppPathService.getBaseUrl( request ) + URL_FORM_BUILDER );
        url.addParameter( FormsConstants.PARAMETER_ID_FORM, nIdForm );
        return redirect( request, url.getUrl( ) );
    }

    /**
     * Builds a wizard screen
     */
    private String getWizardPage( Models model, int nWizardStep, String strTemplate )
    {
        model.put( MARK_DRAFT, _draft );
        model.put( MARK_WIZARD_STEP, nWizardStep );
        return getPage( PROPERTY_PAGE_TITLE, strTemplate, model );
    }

    /**
     * The first screen not completed before the given one, null if the given screen can be shown
     */
    private String getMissingView( String strView )
    {
        if ( _draft == null || !_draft.isFormInfoDone( ) )
        {
            return VIEW_FORM_INFO;
        }
        if ( !VIEW_STRUCTURE.equals( strView ) && !_draft.isStructureDone( ) )
        {
            return VIEW_STRUCTURE;
        }
        if ( VIEW_SUMMARY.equals( strView ) && !_draft.isStepsDone( ) )
        {
            return VIEW_STEPS;
        }
        return null;
    }

    /**
     * Reads the steps posted by the steps screen into the draft
     */
    private void readSteps( HttpServletRequest request )
    {
        readStep( _draft.getFirstStep( ), request, PREFIX_FIRST_STEP );
        if ( !_draft.isMultiStep( ) )
        {
            return;
        }
        readStep( _draft.getLastStep( ), request, PREFIX_LAST_STEP );

        int nCount = Math.min( NumberUtils.toInt( request.getParameter( PARAMETER_INTERMEDIATE_COUNT ), 0 ), MAX_INTERMEDIATE_STEPS );
        List<WizardStepDraft> listSteps = new ArrayList<>( );
        for ( int i = 0; i < nCount; i++ )
        {
            WizardStepDraft step = new WizardStepDraft( );
            readStep( step, request, PREFIX_INTERMEDIATE_STEP + i );
            listSteps.add( step );
        }
        _draft.setIntermediateSteps( listSteps );
    }

    /**
     * Reads a step: its title and description, and the template chosen when the step is built from a template
     */
    private static void readStep( WizardStepDraft step, HttpServletRequest request, String strPrefix )
    {
        step.setTitle( StringUtils.trimToEmpty( request.getParameter( strPrefix + PARAMETER_SUFFIX_TITLE ) ) );
        step.setDescription( StringUtils.trimToEmpty( request.getParameter( strPrefix + PARAMETER_SUFFIX_DESCRIPTION ) ) );
        boolean bFromTemplate = TYPE_TEMPLATE.equals( request.getParameter( strPrefix + PARAMETER_SUFFIX_TYPE ) );
        step.setIdTemplate( bFromTemplate ? NumberUtils.toInt( request.getParameter( strPrefix + PARAMETER_SUFFIX_TEMPLATE ), 0 ) : 0 );
    }

    /**
     * Adds, removes or moves an intermediate step
     *
     * @return true if the operation edited the list of the intermediate steps
     */
    private boolean editIntermediateSteps( String strOperation )
    {
        List<WizardStepDraft> listSteps = _draft.getIntermediateSteps( );
        if ( OPERATION_ADD.equals( strOperation ) )
        {
            if ( listSteps.size( ) < MAX_INTERMEDIATE_STEPS )
            {
                listSteps.add( new WizardStepDraft( ) );
            }
            return true;
        }
        int nIndex;
        if ( strOperation.startsWith( OPERATION_REMOVE ) )
        {
            nIndex = NumberUtils.toInt( strOperation.substring( OPERATION_REMOVE.length( ) ), -1 );
            if ( nIndex >= 0 && nIndex < listSteps.size( ) )
            {
                listSteps.remove( nIndex );
            }
            return true;
        }
        if ( strOperation.startsWith( OPERATION_UP ) )
        {
            nIndex = NumberUtils.toInt( strOperation.substring( OPERATION_UP.length( ) ), -1 );
            if ( nIndex > 0 && nIndex < listSteps.size( ) )
            {
                Collections.swap( listSteps, nIndex, nIndex - 1 );
            }
            return true;
        }
        if ( strOperation.startsWith( OPERATION_DOWN ) )
        {
            nIndex = NumberUtils.toInt( strOperation.substring( OPERATION_DOWN.length( ) ), -1 );
            if ( nIndex >= 0 && nIndex < listSteps.size( ) - 1 )
            {
                Collections.swap( listSteps, nIndex, nIndex + 1 );
            }
            return true;
        }
        return false;
    }

    /**
     * A new draft, with the defaults of the forms creation screen
     */
    private FormWizardDraft newDraft( )
    {
        FormWizardDraft draft = new FormWizardDraft( );
        ReferenceList listBreadcrumbs = BreadcrumbManager.getRefListBreadcrumb( );
        if ( !listBreadcrumbs.isEmpty( ) )
        {
            draft.getForm( ).setBreadcrumbName( listBreadcrumbs.get( 0 ).getCode( ) );
        }
        draft.getForm( ).setWorkgroup( getDefaultWorkgroup( ) );
        draft.getFormMessage( ).setEndMessageDisplay( false );
        draft.getFormMessage( ).setEndMessage( I18nService.getLocalizedString( FormsConstants.FORM_DEFAULT_END_MESSAGE, getLocale( ) ) );
        return draft;
    }

    /**
     * The workgroup of the user when they belong to a single one, all the workgroups otherwise, as in the forms creation screen
     */
    private String getDefaultWorkgroup( )
    {
        ReferenceList listWorkgroups = AdminWorkgroupService.getUserWorkgroups( getUser( ), getLocale( ) );
        if ( listWorkgroups.size( ) == 2 )
        {
            return listWorkgroups.stream( ).map( ReferenceItem::getCode ).filter( code -> !AdminWorkgroupService.ALL_GROUPS.equals( code ) ).findFirst( )
                    .orElse( AdminWorkgroupService.ALL_GROUPS );
        }
        return AdminWorkgroupService.ALL_GROUPS;
    }

    /**
     * Puts the workgroups and the access controls the user can choose into the model
     *
     * @param model
     *            the model
     */
    private void putAccessLists( Models model )
    {
        model.put( MARK_WORKGROUP_LIST, AdminWorkgroupService.getUserWorkgroups( getUser( ), getLocale( ) ) );
        model.put( MARK_ACCESS_CONTROL_LIST, _formWizardService.getAccessControls( getUser( ), getLocale( ) ) );
    }

    /**
     * Tells whether the user can give a form to the workgroup: one of their workgroups, or all the workgroups
     *
     * @param strWorkgroup
     *            the workgroup key
     * @return true if the workgroup is offered to the user
     */
    private boolean isUserWorkgroup( String strWorkgroup )
    {
        return strWorkgroup != null
                && AdminWorkgroupService.getUserWorkgroups( getUser( ), getLocale( ) ).stream( ).anyMatch( item -> strWorkgroup.equals( item.getCode( ) ) );
    }

    /**
     * Tells whether the breadcrumb is one of the breadcrumbs offered
     *
     * @param strBreadcrumb
     *            the breadcrumb key
     * @return true if the breadcrumb is offered
     */
    private static boolean isKnownBreadcrumb( String strBreadcrumb )
    {
        return strBreadcrumb != null && BreadcrumbManager.getRefListBreadcrumb( ).stream( ).anyMatch( item -> strBreadcrumb.equals( item.getCode( ) ) );
    }

    private void checkCreatePermission( ) throws AccessDeniedException
    {
        if ( !RBACService.isAuthorized( Form.RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID, FormsResourceIdService.PERMISSION_CREATE, (User) getUser( ) ) )
        {
            throw new AccessDeniedException( "Form wizard: the user is not allowed to create a form" );
        }
    }
}
