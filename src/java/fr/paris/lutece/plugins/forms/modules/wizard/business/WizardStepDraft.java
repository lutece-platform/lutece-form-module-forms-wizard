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

/**
 * A step described in the wizard, before the form is created: either an empty step (title and description) or a step imported from a step template
 */
public class WizardStepDraft implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String _strTitle;
    private String _strDescription;
    private int _nIdTemplate;
    private String _strTemplateTitle;

    /**
     * @return the title of the step
     */
    public String getTitle( )
    {
        return _strTitle;
    }

    /**
     * @param strTitle
     *            the title of the step
     */
    public void setTitle( String strTitle )
    {
        _strTitle = strTitle;
    }

    /**
     * @return the description of the step
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * @param strDescription
     *            the description of the step
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * @return the step template to import, 0 for an empty step
     */
    public int getIdTemplate( )
    {
        return _nIdTemplate;
    }

    /**
     * @param nIdTemplate
     *            the step template to import, 0 for an empty step
     */
    public void setIdTemplate( int nIdTemplate )
    {
        _nIdTemplate = nIdTemplate;
    }

    /**
     * @return the title of the step template, shown in the summary
     */
    public String getTemplateTitle( )
    {
        return _strTemplateTitle;
    }

    /**
     * @param strTemplateTitle
     *            the title of the step template, shown in the summary
     */
    public void setTemplateTitle( String strTemplateTitle )
    {
        _strTemplateTitle = strTemplateTitle;
    }

    /**
     * @return true if the step is imported from a step template
     */
    public boolean isFromTemplate( )
    {
        return _nIdTemplate > 0;
    }
}
