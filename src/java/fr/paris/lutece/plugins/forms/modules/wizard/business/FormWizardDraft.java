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
package fr.paris.lutece.plugins.forms.modules.wizard.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.paris.lutece.plugins.forms.business.Form;
import fr.paris.lutece.plugins.forms.business.FormMessage;

/**
 * The choices made in the form creation wizard, kept in the session until the form is created
 */
public class FormWizardDraft implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Form _form = new Form( );
    private FormMessage _formMessage = new FormMessage( );
    private int _nIdAccessControl = -1;
    private boolean _bFormInfoDone;
    private boolean _bStructureDone;
    private boolean _bMultiStep;
    private boolean _bStepsDone;
    private WizardStepDraft _firstStep = new WizardStepDraft( );
    private WizardStepDraft _lastStep = new WizardStepDraft( );
    private List<WizardStepDraft> _listIntermediateSteps = new ArrayList<>( );

    /**
     * @return the form to create
     */
    public Form getForm( )
    {
        return _form;
    }

    /**
     * @param form
     *            the form to create
     */
    public void setForm( Form form )
    {
        _form = form;
    }

    /**
     * @return the messages of the form to create
     */
    public FormMessage getFormMessage( )
    {
        return _formMessage;
    }

    /**
     * @param formMessage
     *            the messages of the form to create
     */
    public void setFormMessage( FormMessage formMessage )
    {
        _formMessage = formMessage;
    }

    /**
     * @return the access control to link to the form, -1 for none
     */
    public int getIdAccessControl( )
    {
        return _nIdAccessControl;
    }

    /**
     * @param nIdAccessControl
     *            the access control to link to the form, -1 for none
     */
    public void setIdAccessControl( int nIdAccessControl )
    {
        _nIdAccessControl = nIdAccessControl;
    }

    /**
     * @return true once the form information screen is valid
     */
    public boolean isFormInfoDone( )
    {
        return _bFormInfoDone;
    }

    /**
     * @param bFormInfoDone
     *            true once the form information screen is valid
     */
    public void setFormInfoDone( boolean bFormInfoDone )
    {
        _bFormInfoDone = bFormInfoDone;
    }

    /**
     * @return true once the structure (single or multiple steps) is chosen
     */
    public boolean isStructureDone( )
    {
        return _bStructureDone;
    }

    /**
     * @param bStructureDone
     *            true once the structure (single or multiple steps) is chosen
     */
    public void setStructureDone( boolean bStructureDone )
    {
        _bStructureDone = bStructureDone;
    }

    /**
     * @return true for a form of several steps, false for a single step form
     */
    public boolean isMultiStep( )
    {
        return _bMultiStep;
    }

    /**
     * @param bMultiStep
     *            true for a form of several steps, false for a single step form
     */
    public void setMultiStep( boolean bMultiStep )
    {
        _bMultiStep = bMultiStep;
    }

    /**
     * @return true once the steps screen is valid
     */
    public boolean isStepsDone( )
    {
        return _bStepsDone;
    }

    /**
     * @param bStepsDone
     *            true once the steps screen is valid
     */
    public void setStepsDone( boolean bStepsDone )
    {
        _bStepsDone = bStepsDone;
    }

    /**
     * @return the first step, the only one of a single step form
     */
    public WizardStepDraft getFirstStep( )
    {
        return _firstStep;
    }

    /**
     * @param firstStep
     *            the first step, the only one of a single step form
     */
    public void setFirstStep( WizardStepDraft firstStep )
    {
        _firstStep = firstStep;
    }

    /**
     * @return the last step of a multiple steps form
     */
    public WizardStepDraft getLastStep( )
    {
        return _lastStep;
    }

    /**
     * @param lastStep
     *            the last step of a multiple steps form
     */
    public void setLastStep( WizardStepDraft lastStep )
    {
        _lastStep = lastStep;
    }

    /**
     * @return the steps between the first and the last step, in their order
     */
    public List<WizardStepDraft> getIntermediateSteps( )
    {
        return _listIntermediateSteps;
    }

    /**
     * @param listIntermediateSteps
     *            the steps between the first and the last step, in their order
     */
    public void setIntermediateSteps( List<WizardStepDraft> listIntermediateSteps )
    {
        _listIntermediateSteps = listIntermediateSteps;
    }
}
