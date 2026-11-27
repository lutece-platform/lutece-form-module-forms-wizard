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

import fr.paris.lutece.plugins.forms.service.json.IStepTemplateProvider;
import fr.paris.lutece.plugins.forms.service.json.StepJsonData;
import fr.paris.lutece.util.ReferenceList;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Step template provider of the tests, standing for the module forms-template: it offers the one template the test gives it
 */
@ApplicationScoped
public class TestStepTemplateProvider implements IStepTemplateProvider
{
    public static final int ID_TEMPLATE = 4242;
    public static final String TITLE_TEMPLATE = "Wizard test template";

    private StepJsonData _template;

    /**
     * @param template
     *            the template offered, null for none
     */
    public void setTemplate( StepJsonData template )
    {
        _template = template;
    }

    @Override
    public ReferenceList getStepTemplateList( )
    {
        ReferenceList list = new ReferenceList( );
        if ( _template != null )
        {
            list.addItem( ID_TEMPLATE, TITLE_TEMPLATE );
        }
        return list;
    }

    @Override
    public StepJsonData getStepTemplateData( int idTemplate )
    {
        return idTemplate == ID_TEMPLATE ? _template : null;
    }

    @Override
    public String getProviderBeanName( )
    {
        return "forms-wizard.testStepTemplateProvider";
    }
}
