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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.forms.business.CompositeDisplayType;
import fr.paris.lutece.plugins.forms.business.Form;
import fr.paris.lutece.plugins.forms.business.FormDisplay;
import fr.paris.lutece.plugins.forms.business.FormDisplayHome;
import fr.paris.lutece.plugins.forms.business.FormHome;
import fr.paris.lutece.plugins.forms.business.FormMessageHome;
import fr.paris.lutece.plugins.forms.business.Group;
import fr.paris.lutece.plugins.forms.business.GroupHome;
import fr.paris.lutece.plugins.forms.business.Step;
import fr.paris.lutece.plugins.forms.business.StepHome;
import fr.paris.lutece.plugins.forms.business.Transition;
import fr.paris.lutece.plugins.forms.business.TransitionHome;
import fr.paris.lutece.plugins.forms.modules.wizard.business.FormWizardDraft;
import fr.paris.lutece.plugins.forms.modules.wizard.business.WizardStepDraft;
import fr.paris.lutece.plugins.forms.service.FormService;
import fr.paris.lutece.plugins.forms.service.json.FormJsonService;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.test.LuteceTestCase;
import jakarta.inject.Inject;

/**
 * Tests of the creation of a form by the wizard
 */
public class FormWizardServiceTest extends LuteceTestCase
{
    private static final String BREADCRUMB = "forms.horizontalBreadcrumb";
    private static final String TITLE_PREFIX = "wizard_test_";

    @Inject
    private FormWizardService _formWizardService;
    @Inject
    private FormService _formService;
    @Inject
    private FormJsonService _formJsonService;
    @Inject
    private TestStepTemplateProvider _templateProvider;

    private final List<Integer> _listCreatedForms = new ArrayList<>( );

    /**
     * No template is offered unless the test gives one
     */
    @BeforeEach
    public void resetTemplate( )
    {
        _templateProvider.setTemplate( null );
    }

    /**
     * Removes the forms created by the test, with their steps, transitions and displays
     */
    @AfterEach
    public void removeForms( )
    {
        for ( int nIdForm : _listCreatedForms )
        {
            _formService.removeForm( nIdForm, new AdminUser( ) );
        }
        _listCreatedForms.clear( );
        _templateProvider.setTemplate( null );
    }

    /**
     * A single step form gets one step, both initial and final, and no transition
     */
    @Test
    public void testCreateSingleStepForm( )
    {
        FormWizardDraft draft = newDraft( "single", false );
        draft.setFirstStep( newStep( "The only step" ) );

        int nIdForm = create( draft );

        Form form = FormHome.findByPrimaryKey( nIdForm );
        assertEquals( TITLE_PREFIX + "single", form.getTitle( ) );
        assertEquals( BREADCRUMB, form.getBreadcrumbName( ) );
        assertNotNull( FormMessageHome.findByForm( nIdForm ) );
        List<Step> listSteps = StepHome.getStepsListByForm( nIdForm );
        assertEquals( 1, listSteps.size( ) );
        assertTrue( listSteps.get( 0 ).isInitial( ) );
        assertTrue( listSteps.get( 0 ).isFinal( ) );
        assertEquals( "The only step", listSteps.get( 0 ).getTitle( ) );
        assertTrue( TransitionHome.getTransitionsListFromForm( nIdForm ).isEmpty( ) );
    }

    /**
     * A multiple steps form chains its steps in order: first, intermediates, last
     */
    @Test
    public void testCreateMultiStepFormChainsSteps( )
    {
        FormWizardDraft draft = newDraft( "multi", true );
        draft.setFirstStep( newStep( "First" ) );
        draft.getIntermediateSteps( ).add( newStep( "Middle 1" ) );
        draft.getIntermediateSteps( ).add( newStep( "Middle 2" ) );
        draft.setLastStep( newStep( "Last" ) );

        int nIdForm = create( draft );

        List<Step> listSteps = StepHome.getStepsListByForm( nIdForm );
        assertEquals( 4, listSteps.size( ) );
        assertEquals( 1, listSteps.stream( ).filter( Step::isInitial ).count( ) );
        assertEquals( 1, listSteps.stream( ).filter( Step::isFinal ).count( ) );
        assertEquals( List.of( "First", "Middle 1", "Middle 2", "Last" ), followChain( nIdForm ) );
    }

    /**
     * An intermediate step from a template brings the questions of the template and keeps the chain of the steps
     *
     * @throws Exception
     *             if the template cannot be exported
     */
    @Test
    public void testCreateWithTemplateIntermediate( ) throws Exception
    {
        int nIdSource = createTemplateSource( );
        Step sourceStep = StepHome.getStepsListByForm( nIdSource ).get( 0 );
        _templateProvider.setTemplate( _formJsonService.jsonExportStepAsData( nIdSource, sourceStep.getId( ) ) );

        FormWizardDraft draft = newDraft( "template", true );
        draft.setFirstStep( newStep( "First" ) );
        WizardStepDraft templateStep = new WizardStepDraft( );
        templateStep.setIdTemplate( TestStepTemplateProvider.ID_TEMPLATE );
        draft.getIntermediateSteps( ).add( templateStep );
        draft.setLastStep( newStep( "Last" ) );

        assertTrue( _formWizardService.isTemplateAvailable( ) );
        assertTrue( _formWizardService.validateSteps( draft, Locale.ENGLISH ).isEmpty( ) );
        assertEquals( TestStepTemplateProvider.TITLE_TEMPLATE, templateStep.getTemplateTitle( ) );

        int nIdForm = create( draft );

        assertEquals( List.of( "First", TestStepTemplateProvider.TITLE_TEMPLATE, "Last" ), followChain( nIdForm ) );
        Step imported = StepHome.getStepsListByForm( nIdForm ).stream( ).filter( step -> TestStepTemplateProvider.TITLE_TEMPLATE.equals( step.getTitle( ) ) ).findFirst( )
                .orElseThrow( );
        assertFalse( imported.isInitial( ) );
        assertFalse( imported.isFinal( ) );
        assertEquals( 1, FormDisplayHome.getFormDisplayListByParent( imported.getId( ), 0 ).size( ) );
    }

    /**
     * The first and the last steps can come from a template: they keep their place (initial, final) and take the name of the template
     *
     * @throws Exception
     *             if the template cannot be exported
     */
    @Test
    public void testCreateWithTemplateFirstAndLastSteps( ) throws Exception
    {
        offerTemplate( );
        FormWizardDraft draft = newDraft( "template_ends", true );
        draft.setFirstStep( newTemplateStep( ) );
        draft.getIntermediateSteps( ).add( newStep( "Middle" ) );
        draft.setLastStep( newTemplateStep( ) );
        assertTrue( _formWizardService.validateSteps( draft, Locale.ENGLISH ).isEmpty( ) );

        int nIdForm = create( draft );

        assertEquals( List.of( TestStepTemplateProvider.TITLE_TEMPLATE, "Middle", TestStepTemplateProvider.TITLE_TEMPLATE ), followChain( nIdForm ) );
        List<Step> listSteps = StepHome.getStepsListByForm( nIdForm );
        assertEquals( 1, listSteps.stream( ).filter( Step::isInitial ).count( ) );
        assertEquals( 1, listSteps.stream( ).filter( Step::isFinal ).count( ) );
        assertTrue( StepHome.getInitialStep( nIdForm ).getTitle( ).equals( TestStepTemplateProvider.TITLE_TEMPLATE ) );
        for ( Step step : listSteps )
        {
            int nExpectedDisplays = "Middle".equals( step.getTitle( ) ) ? 0 : 1;
            assertEquals( nExpectedDisplays, FormDisplayHome.getFormDisplayListByParent( step.getId( ), 0 ).size( ) );
        }
    }

    /**
     * The step of a single step form can come from a template: it is both initial and final
     *
     * @throws Exception
     *             if the template cannot be exported
     */
    @Test
    public void testCreateSingleStepFromTemplate( ) throws Exception
    {
        offerTemplate( );
        FormWizardDraft draft = newDraft( "single_template", false );
        draft.setFirstStep( newTemplateStep( ) );
        assertTrue( _formWizardService.validateSteps( draft, Locale.ENGLISH ).isEmpty( ) );

        int nIdForm = create( draft );

        List<Step> listSteps = StepHome.getStepsListByForm( nIdForm );
        assertEquals( 1, listSteps.size( ) );
        assertTrue( listSteps.get( 0 ).isInitial( ) );
        assertTrue( listSteps.get( 0 ).isFinal( ) );
        assertEquals( TestStepTemplateProvider.TITLE_TEMPLATE, listSteps.get( 0 ).getTitle( ) );
        assertTrue( TransitionHome.getTransitionsListFromForm( nIdForm ).isEmpty( ) );
    }

    private void offerTemplate( ) throws Exception
    {
        int nIdSource = createTemplateSource( );
        Step sourceStep = StepHome.getStepsListByForm( nIdSource ).get( 0 );
        _templateProvider.setTemplate( _formJsonService.jsonExportStepAsData( nIdSource, sourceStep.getId( ) ) );
    }

    private static WizardStepDraft newTemplateStep( )
    {
        WizardStepDraft step = new WizardStepDraft( );
        step.setIdTemplate( TestStepTemplateProvider.ID_TEMPLATE );
        return step;
    }

    /**
     * The steps without title and the templates no longer offered are reported
     */
    @Test
    public void testValidateSteps( )
    {
        FormWizardDraft draft = newDraft( "invalid", true );
        draft.setFirstStep( newStep( "" ) );
        draft.getIntermediateSteps( ).add( newStep( " " ) );
        WizardStepDraft templateStep = new WizardStepDraft( );
        templateStep.setIdTemplate( TestStepTemplateProvider.ID_TEMPLATE );
        draft.getIntermediateSteps( ).add( templateStep );
        draft.setLastStep( newStep( null ) );

        assertEquals( 4, _formWizardService.validateSteps( draft, Locale.ENGLISH ).size( ) );

        draft.setMultiStep( false );
        assertEquals( 1, _formWizardService.validateSteps( draft, Locale.ENGLISH ).size( ) );
    }

    /**
     * Without an access control service, no access control is offered and only none (-1) can be linked
     */
    @Test
    public void testAccessControlWithoutService( )
    {
        AdminUser user = new AdminUser( );
        assertTrue( _formWizardService.getAccessControls( user, Locale.ENGLISH ).isEmpty( ) );
        assertTrue( _formWizardService.isAccessControlOffered( -1, user, Locale.ENGLISH ) );
        assertFalse( _formWizardService.isAccessControlOffered( 1, user, Locale.ENGLISH ) );
    }

    /**
     * A template that cannot be imported stops the creation with the wizard error
     */
    @Test
    public void testCreateWithMissingTemplateFails( )
    {
        FormWizardDraft draft = newDraft( "missing_template", true );
        draft.setFirstStep( newStep( "First" ) );
        WizardStepDraft templateStep = new WizardStepDraft( );
        templateStep.setIdTemplate( TestStepTemplateProvider.ID_TEMPLATE );
        draft.getIntermediateSteps( ).add( templateStep );
        draft.setLastStep( newStep( "Last" ) );

        FormWizardException exception = assertThrows( FormWizardException.class, ( ) -> _formWizardService.createForm( draft, Locale.ENGLISH ) );
        assertEquals( FormWizardService.MESSAGE_ERROR_CREATION, exception.getMessage( ) );
        // Without a container transaction the rows written before the failure stay: remove them
        FormHome.getFormList( ).stream( ).filter( form -> ( TITLE_PREFIX + "missing_template" ).equals( form.getTitle( ) ) )
                .forEach( form -> _listCreatedForms.add( form.getId( ) ) );
    }

    private int create( FormWizardDraft draft )
    {
        int nIdForm = _formWizardService.createForm( draft, Locale.ENGLISH );
        _listCreatedForms.add( nIdForm );
        return nIdForm;
    }

    /**
     * The titles of the steps met from the initial step, following the first transition of each step
     */
    private static List<String> followChain( int nIdForm )
    {
        List<String> listTitles = new ArrayList<>( );
        Step step = StepHome.getInitialStep( nIdForm );
        while ( step != null && listTitles.size( ) < 10 )
        {
            listTitles.add( step.getTitle( ) );
            List<Transition> listTransitions = TransitionHome.getTransitionsListFromStep( step.getId( ) );
            step = listTransitions.isEmpty( ) ? null : StepHome.findByPrimaryKey( listTransitions.get( 0 ).getNextStep( ) );
        }
        return listTitles;
    }

    /**
     * A form whose single step holds one group, exported as the template
     */
    private int createTemplateSource( )
    {
        Form form = new Form( );
        form.setTitle( TITLE_PREFIX + "source" );
        form.setAvailabilityStartDate( new Timestamp( System.currentTimeMillis( ) ) );
        form.setBreadcrumbName( BREADCRUMB );
        FormHome.create( form );
        _listCreatedForms.add( form.getId( ) );

        Step step = new Step( );
        step.setIdForm( form.getId( ) );
        step.setTitle( "Source step" );
        step.setInitial( true );
        step.setFinal( true );
        StepHome.create( step );

        Group group = new Group( );
        group.setTitle( "Source group" );
        group.setIdStep( step.getId( ) );
        group.setIterationMin( 1 );
        group.setIterationMax( 1 );
        GroupHome.create( group );

        FormDisplay display = new FormDisplay( );
        display.setFormId( form.getId( ) );
        display.setStepId( step.getId( ) );
        display.setParentId( 0 );
        display.setCompositeId( group.getId( ) );
        display.setCompositeType( CompositeDisplayType.GROUP.getLabel( ) );
        FormDisplayHome.create( display );
        return form.getId( );
    }

    private static FormWizardDraft newDraft( String strName, boolean bMultiStep )
    {
        FormWizardDraft draft = new FormWizardDraft( );
        draft.getForm( ).setTitle( TITLE_PREFIX + strName );
        draft.getForm( ).setBreadcrumbName( BREADCRUMB );
        draft.getFormMessage( ).setEndMessageDisplay( true );
        draft.getFormMessage( ).setEndMessage( "Thanks" );
        draft.setMultiStep( bMultiStep );
        return draft;
    }

    private static WizardStepDraft newStep( String strTitle )
    {
        WizardStepDraft step = new WizardStepDraft( );
        step.setTitle( strTitle );
        return step;
    }
}
