/*
 * Copyright (c) 2010 iceScrum Technologies.
 *
 * This file is part of iceScrum.
 *
 * iceScrum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * iceScrum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with iceScrum.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Authors:
 *
 * Vincent Barrier (vbarrier@kagilum.com)
 * Stéphane Maldini (stephane.maldini@icescrum.com)
 *
 */

package org.icescrum.presentation.taglib

import org.springframework.web.servlet.support.RequestContextUtils as RCU

import grails.converters.JSON
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.icescrum.components.UtilsWebComponents
import org.icescrum.core.domain.security.Authority
import org.icescrum.core.support.ApplicationSupport
import org.icescrum.web.presentation.app.project.ActorController
import org.icescrum.core.utils.BundleUtils
import org.apache.commons.lang.StringEscapeUtils
import groovy.xml.MarkupBuilder
import org.springframework.validation.Errors
import grails.plugin.springcache.annotations.Cacheable
import grails.plugin.springcache.key.CacheKeyBuilder
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack
import org.codehaus.groovy.grails.web.pages.FastStringWriter
import grails.plugin.springcache.taglib.ResultAndBuffer

class UtilsTagLib {

    static namespace = 'is'

    def grailsApplication
    def springcacheService
    def loadJsVar = { attrs, body ->
        def opts = ''
        def current
        if (params.product) {
            current = pageScope.product
            opts = """currentProductName :'${current.name.encodeAsJavaScript()}',"""

        }
        if (params.team) {
            current = pageScope.team
            opts = """currentTeamName :'${current.name.encodeAsJavaScript()}',"""

        }
        def p = []
        def widgetsList = session.widgetsList
        def controllerSpace = 'scrumOS'
        if (params.product) {
            p = [product: current.id]
        } else if (params.team) {
            p = [team: current.id]
            controllerSpace = 'team'
            widgetsList = session.widgetsTeamList
        }

        def locale = attrs.locale ? attrs.locale.replace('_', '-') : RCU.getLocale(request).toString().replace('_', '-')
        def jsCode = """var icescrum = {
                          grailsServer:"${grailsApplication.config.grails.serverURL}",
                          baseUrl: "${createLink(controller: 'scrumOS')}",
                          baseUrlProduct: ${p ? '\'' + createLink(controller: controllerSpace, params: p) + '/\'' : null},
                          streamUrl: "${createLink(controller: 'scrumOS')}stream/app${params.product ? '?product=' + current.id : params.team ? '?team=' + current.id : '' }",
                          ${opts}
                          urlOpenWidget:"${createLink(controller: controllerSpace, action: 'openWidget', params: p)}",
                          urlCloseWidget:"${createLink(controller: controllerSpace, action: 'closeWidget', params: p)}",
                          urlOpenWindow:"${createLink(controller: controllerSpace, action: 'openWindow', params: p)}",
                          urlCloseWindow:"${createLink(controller: controllerSpace, action: 'closeWindow', params: p)}",
                          deleteConfirmMessage:"${message(code: 'is.confirm.delete').encodeAsJavaScript()}",
                          cancelFormConfirmMessage:"${message(code: 'is.confirm.cancel.form').encodeAsJavaScript()}",
                          widgetsList:${widgetsList as JSON ?: []},
                          currentView:"${session.currentView ?: 'postitsView'}",
                          locale:'${locale}',
                          push:${grailsApplication.config.icescrum.push.enable?:false},
                          dialogErrorContent:"<div id=\'window-dialog\'><form method=\'post\' class=\'box-form box-form-250 box-form-250-legend\'><div  title=\'${message(code: 'is.dialog.sendError.title')}\' class=\' panel ui-corner-all\'><h3 class=\'panel-title\'>${message(code: 'is.dialog.sendError.title')}</h3><p class=\'field-information\'>${message(code: 'is.dialog.sendError.description')}</p><p class=\'field-area clearfix field-noseparator\' for=\'stackError\' label=\'${message(code: 'is.dialog.sendError.stackError')}\'><label for=\'stackError\'>${message(code: 'is.dialog.sendError.stackError')}</label><span class=\'area area-large\' id=\'stackError-field\'><span class=\'start\'></span><span class=\'content\'><textarea id=\'stackError\' name=\'stackError\' ></textarea></span><span class=\'end\'></span></span></p><p class=\'field-area clearfix field-noseparator\' for=\'comments\' label=\'${message(code: 'is.dialog.sendError.comments')}\'><label for=\'comments\'>${message(code: 'is.dialog.sendError.comments')}</label><span class=\'area area-large\' id=\'comments-field\'><span class=\'start\'></span><span class=\'content\'><textarea id=\'comments\' name=\'comments\' ></textarea></span><span class=\'end\'></span></span></p></div></form></div>"
                };"""
        out << g.javascript(null, jsCode)
    }

    def quickLook = { attrs ->
        def params = [
                action: "index",
                controller: "quickLook",
                width: "600",
                resizable: "false",
                draggable: "false",
                withTitlebar: "false",
                buttons: "'${message(code: 'is.button.close')}': function() { \$(this).dialog('close'); }",
                focusable: false,
                params: attrs.params
        ]
        out << is.remoteDialogFunction(params)
    }

    /**
     * Generate the iceScrum desktop (where the main window appear)
     */
    def desktop = { attrs, body ->
        out << '<div id="main">'
        out << '<div id="main-content">'
        out << body()
        out << '</div>'
        out << '</div>'
    }

    def simpleDesktop = { attrs, body ->
        out << '<div id="main-simple">'
        out << '<div id="main-content">'
        out << body()
        out << '</div>'
        out << '</div>'
    }

    def changeRank = { attrs, body ->

        if (attrs.params && attrs.params instanceof Map)
            attrs.addParams = "params = params+'&${UtilsWebComponents.createQueryString(attrs.params)}';"
        else if (attrs.params)
            attrs.addParams = "params = params+'${attrs.params}';"
        out << """\$.icescrum.changeRank(${attrs.selector ? '\'' + attrs.selector + '\'' : 'container'}, this, ${attrs.ui ?: 'ui.item'}, '${attrs.name?:'position'}',function(params, ui) { ${attrs.addParams ?: ''}
    ${
            remoteFunction(
                    action: attrs.action,
                    controller: attrs.controller,
                    id: attrs.id,
                    params: 'params',
                    onFailure: "\$(ui).sortable(\"cancel\");" + attrs.onFailure)
        }})"""
    }

    /**
     * Generate iceScrum main menu (project dropMenu, avatar, roles, logout, ...)
     */
    def mainMenu = { attrs, body ->
        out << g.render(template: '/scrumOS/navigation',
                model: [
                        importEnable: (ApplicationSupport.booleanValue(grailsApplication.config.icescrum.project.import.enable) || SpringSecurityUtils.ifAnyGranted(Authority.ROLE_ADMIN)),
                        exportEnable: (ApplicationSupport.booleanValue(grailsApplication.config.icescrum.project.export.enable) || SpringSecurityUtils.ifAnyGranted(Authority.ROLE_ADMIN)),
                        creationProjectEnable: (ApplicationSupport.booleanValue(grailsApplication.config.icescrum.project.creation.enable) || SpringSecurityUtils.ifAnyGranted(Authority.ROLE_ADMIN)),
                ]
        )
    }

    def savedRequest = {attrs, body ->
        if (params.ref) {
            out << params.ref.decodeURL()
        } else if (session.SPRING_SECURITY_SAVED_REQUEST_KEY)
            out << session.SPRING_SECURITY_SAVED_REQUEST_KEY.requestURL
        else
            out << createLink(uri: '/')
    }

    def bundle = {attrs, body ->
        out << g.message(code: BundleUtils."${attrs.bundle}".get(attrs.value))
    }

    def tooltipSprint = { attrs ->

        def hideText
        def hideBorder

        if (!attrs.text) {
            hideText = "qtip-text-hidden"
            hideBorder = "{'border':'none'}"
        }

        def params = [
                for: "#event-id-${attrs.id} .event-header-label",
                contentTitleText: attrs.title,
                contentText: attrs.text,
                styleName: "icescrum",
                positionTarget: "\'mouse\'",
                positionAdjustMouse: "false",
                positionAdjustX: "5",
                positionAdjustY: "5",
                showSolo: "true",
                showDelay: "500",
                hideDelay: "0",
                styleTitle: hideBorder ?: null,
                apiBeforeShow: """function(){ if(\$('#dropmenu').is(':visible')){return false;}}""",
                styleClassesContent: hideText ?: null,
                hideWhenEvent: "mouseleave",
                positionContainer: attrs.container
        ]
        out << is.tooltip(params)
    }

    def tooltipPostit = { attrs ->

        def hideText
        def hideBorder

        if (!attrs.text) {
            hideText = "qtip-text-hidden"
            hideBorder = "{'border':'none'}"
        }

        def params = [
                for: "div.#postit-${attrs.type}-${attrs.id}",
                contentTitleText: attrs.title,
                contentText: attrs.text,
                styleName: "icescrum",
                positionTarget: "\'mouse\'",
                positionAdjustMouse: "false",
                positionAdjustScreen: "true",
                positionAdjustX: "10",
                positionAdjustY: "10",
                styleTypeSizeY: "200",
                showSolo: "true",
                showDelay: "500",
                hideDelay: "0",
                styleTitle: hideBorder ?: null,
                styleClassesContent: hideText ?: null,
                hideWhenEvent: "mouseleave",
                positionContainer: attrs.container,
                apiBeforeShow: "function(){ if (jQuery('#postit-${attrs.type}-${attrs.id}').hasClass('ui-sortable-helper')) { return false; }; ${attrs.apiBeforeShow}}"
        ]
        out << is.tooltip(params)
    }

    def avatar = { attrs, body ->
        assert attrs.user
        if (grailsApplication.config.icescrum.gravatar){
            def hash = attrs.user.email.encodeAsMD5()
            def dgu = createLink(uri: '/' + is.currentThemeImage()) + "avatars/avatar.png"
            def gravatarBaseUrl = request.isSecure() ? "https://secure.gravatar.com/avatar/" : "http://gravatar.com/avatar/"
            String gravatarUrl = "$gravatarBaseUrl$hash"
            gravatarUrl += dgu.matches(/404|mm|identicon|monsterid|wavatar|retro|http.*/) ? "?d=${dgu}&s=40" : ''
            if (attrs.link){
                out << gravatarUrl
            }else {
                out << "<img src='$gravatarUrl' height='40' width='40' class='avatar avatar-user-${attrs.user.id} ${attrs."class" ? attrs."class" : ''}' title='${message(code: "is.user.avatar")}' alt='${message(code: "is.user.avatar")}'/>"
            }
        }
        else {
            def avat = new File(grailsApplication.config.icescrum.images.users.dir + attrs.user.id + '.png')
            if (avat.exists()) {
                if (attrs.link){
                    out << createLink(controller: 'user', action: 'avatar', id: attrs.user.id) + (attrs.nocache ? '?nocache=' + new Date().getTime() : '')
                }else{
                    out << "<img src='${createLink(controller: 'user', action: 'avatar', id: attrs.user.id)}${attrs.nocache ? '?nocache=' + new Date().getTime() : ''}' ${attrs.elementId ? 'id=\'' + attrs.elementId + '\'' : ''} class='avatar avatar-user-${attrs.user.id} ${attrs."class" ? attrs."class" : ''}' title='${message(code: "is.user.avatar")}' alt='${message(code: "is.user.avatar")}'/>"
                }
            } else {
                if (attrs.link){
                    out <<  "${grailsApplication.config.grails.serverURL}/${is.currentThemeImage()}avatars/avatar.png"
                }else{
                    out << r.img(
                            id: attrs.elementId ?: '',
                            uri: "/${is.currentThemeImage()}avatars/avatar.png",
                            class: "avatar avatar-user-${attrs.user.id} ${attrs."class" ? attrs."class" : ''}",
                            title: message(code: "is.user.avatar")
                    )
                }
            }
        }
    }
    def avatarSelector = { attrs ->
        def avatarsDir = grailsApplication.parentContext.getResource(is.currentThemeImage().toString() + 'avatars').file
        if (avatarsDir.isDirectory()) {
            out << "<span class=\"selector-avatars\">"
            avatarsDir.listFiles().each {
                if (it.name.endsWith('.png')) {
                    out << """<span>
                      <img rel='${it.name}' src=\"${createLink(uri: '/' + is.currentThemeImage())}avatars/${it.name}\" onClick=\"jQuery('#preview-avatar').attr('src',jQuery(this).attr('src'));jQuery('#avatar-selected').val(jQuery(this).attr('rel'));jQuery('#avatar-field input.is-multifiles-uploaded').val('');\"/>
                    </span>"""
                }
            }
            out << "<input type='hidden' id='avatar-selected' name='avatar-selected'/>"
            out << "</span>"
        }
    }

    def bundleLocaleToJs = { attrs ->
        assert attrs.bundle
        def val = []
        attrs.bundle.each {
            val[it.key] = message(code: it.value)
        }
        if (attrs.var) {
            out << "var ${attrs.var} = ${val as JSON};"
        } else {
            out << "${val as JSON}"
        }
    }

    def onStream = { attrs ->
        if (!grailsApplication.config.icescrum.push?.enable){
            return
        }
        def jqCode = ""
        attrs.events.each { it ->
            def events = [];
            it.events.each { event ->
                events << event + '_' + it.object + '.stream'
            }
            jqCode += "jQuery('${attrs.on}').bind('${events.join(' ')}',function(event,${it.object}){ "
            jqCode += "var type = event.type.split('_')[0];"
            jqCode += attrs.constraint ? " if ( ${attrs.constraint} ) { ${attrs.callback ?: "jQuery.icescrum.${it.object}[type].apply(${it.object},['${attrs.template}']);"} " : ""
            jqCode += attrs.constraint ? "}" : attrs.callback ?: " jQuery.icescrum.${it.object}[type].apply(${it.object}${attrs.template ? ',[\'' + attrs.template + '\']' : ''});"
            jqCode += "});"
        }
        out << jq.jquery(null, jqCode)
    }

    /**
     * Loops through each error and renders it using one of the supported mechanisms (defaults to "list" if unsupported).
     *
     * @attr bean REQUIRED The bean to check for errors
     * @attr field The field of the bean or model reference to check
     * @attr model The model reference to check for errors
     */
    def renderErrors = { attrs, body ->
        def renderAs = attrs.remove('as')
        if (!renderAs) renderAs = 'list'

        if (renderAs == 'list') {
            def codec = attrs.codec ?: 'HTML'
            if (codec == 'none') codec = ''

            out << "<ul>"
            out << eachErrorInternal(attrs, {
                out << "<li>${message(error:it, encodeAs:codec)}</li>"
            })
            out << "</ul>"
        }
        else if (renderAs.equalsIgnoreCase("json")) {
            def errors = []
            eachErrorInternal(attrs, {
                    errors << [error:[object: it.objectName,field: it.field, message: message(error:it)?.toString(),'rejected-value': StringEscapeUtils.escapeXml(it.rejectedValue)]]
                })
            out << (errors as JSON).toString()
        }
        else if (renderAs.equalsIgnoreCase("xml")) {
            def mkp = new MarkupBuilder(out)
            mkp.errors() {
                eachErrorInternal(attrs, {
                    error(object: it.objectName,
                          field: it.field,
                          message: message(error:it)?.toString(),
                            'rejected-value': StringEscapeUtils.escapeXml(it.rejectedValue))
                })
            }
        }
    }

    def eachErrorInternal(attrs, body, boolean outputResult = false) {
        def errorsList = extractErrors(attrs)
        def var = attrs.var
        def field = attrs.field

        def errorList = []
        for (errors in errorsList) {
            if (field) {
                if (errors.hasFieldErrors(field)) {
                    errorList += errors.getFieldErrors(field)
                }
            }
            else {
                errorList += errors.allErrors
            }
        }

        for (error in errorList) {
            def result
            if (var) {
                result = body([(var):error])
            }
            else {
                result = body(error)
            }
            if (outputResult) {
                out << result
            }
        }

        null
    }

    def extractErrors(attrs) {
        def model = attrs.model
        def checkList = []
        if (attrs.containsKey('bean')) {
            if (attrs.bean) {
                checkList << attrs.bean
            }
        }
        else if (attrs.containsKey('model')) {
            if (model) {
                checkList = model.findAll {it.value?.errors instanceof Errors}.collect {it.value}
            }
        }
        else {
            for (attributeName in request.attributeNames) {
                def ra = request[attributeName]
                if (ra) {
                    def mc = GroovySystem.metaClassRegistry.getMetaClass(ra.getClass())
                    if (ra instanceof Errors && !checkList.contains(ra)) {
                        checkList << ra
                    }
                    else if (mc.hasProperty(ra, 'errors') && ra.errors instanceof Errors && !checkList.contains(ra.errors)) {
                        checkList << ra.errors
                    }
                }
            }
        }

        def resultErrorsList = []

        for (i in checkList) {
            def errors = null
            if (i instanceof Errors) {
                errors = i
            }
            else {
                def mc = GroovySystem.metaClassRegistry.getMetaClass(i.getClass())
                if (mc.hasProperty(i, 'errors')) {
                    errors = i.errors
                }
            }
            if (errors?.hasErrors()) {
                // if the 'field' attribute is not provided then we should output a body,
                // otherwise we should check for field-specific errors
                if (!attrs.field || errors.hasFieldErrors(attrs.field)) {
                    resultErrorsList << errors
                }
            }
        }

        resultErrorsList
    }

    def cache = { attrs, body ->
        if (attrs.disabled){
            out << body()
            return
        }

        attrs.role = attrs.role ?: true
        attrs.locale = attrs.locale ?: true
        def cacheResolver = ApplicationHolder.application.mainContext[attrs.cacheResolver ?: 'springcacheDefaultCacheResolver']
        def role = ''

        def key  = new CacheKeyBuilder()
        key.append(attrs.key)

        if (attrs.role){
            if (request.admin) {
            role = 'adm'
            } else {
                if (request.scrumMaster)  {  role += 'scm'  }
                if (request.teamMember)   {  role += 'tm'  }
                if (request.productOwner) {  role += 'po'  }
                if (!role && request.stakeHolder) {  role += 'sh'  }
            }
            role = role ?: 'anonymous'
            key.append(role)
        }

        if (attrs.locale)
            key.append(RCU.getLocale(request).toString().substring(0, 2))

        def resultAndBuffer = springcacheService.doWithCache(cacheResolver.resolveCacheName(attrs.cache), key.toCacheKey()) {
            def outputStack = GroovyPageOutputStack.currentStack()
            def writer = new FastStringWriter()
            outputStack.push(writer, true)
            def result = body()
            outputStack.pop()
            new ResultAndBuffer(result: result, buffer: writer.buffer)
        }

        GroovyPageOutputStack.currentWriter() << resultAndBuffer.buffer
        out << resultAndBuffer.result
    }

    def newVersion = {
        def info = grailsApplication.config.icescrum.check?.available?:null
        if (info){
            def v = 'R'+info?.version?.replaceFirst('\\.','#')
            out << """<a href='${info.url}' target='_blank'>
                        <li class='navigation-line new-version' title='${info.message?:g.message(code:'is.new.version')} ${v}'></li>
                      </a>"""
        }
    }

    def appId = {
        out << grailsApplication.config.icescrum.appID
    }
}