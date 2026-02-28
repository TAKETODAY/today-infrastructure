<#--
test template for FreeMarker macro test class
-->
<#import "/infra.ftl" as infra />

NAME
${command.name}

MESSAGE
<@infra.message "hello"/> <@infra.message "world"/>

DEFAULTMESSAGE
<@infra.messageText "no.such.code", "hi"/> <@infra.messageText "no.such.code", "planet"/>

MESSAGEARGS
<@infra.messageArgs "hello", msgArgs/>

MESSAGEARGSWITHDEFAULTMESSAGE
<@infra.messageArgsText "no.such.code", msgArgs, "Hi"/>

URL
<@infra.url "/aftercontext.html"/>

URLPARAMS
<@infra.url relativeUrl="/aftercontext/{foo}?spam={spam}" foo="bar" spam="bucket"/>

FORM1
<@infra.formInput "command.name", ""/>

FORM2
<@infra.formInput "command.name", 'class="myCssClass"'/>

FORM3
<@infra.formTextarea "command.name", ""/>

FORM4
<@infra.formTextarea "command.name", "rows=10 cols=30"/>

FORM5
<@infra.formSingleSelect "command.name", nameOptionMap, ""/>

FORM6
<@infra.formMultiSelect "command.spouses", nameOptionMap, ""/>

FORM7
<@infra.formRadioButtons "command.name", nameOptionMap, " ", ""/>

FORM8
<@infra.formCheckboxes "command.stringArray", nameOptionMap, " ", ""/>

FORM9
<@infra.formPasswordInput "command.name", ""/>

FORM10
<@infra.formHiddenInput "command.name", ""/>

FORM11
<@infra.formInput "command.name", "", "text"/>

FORM12
<@infra.formInput "command.name", "", "hidden"/>

FORM13
<@infra.formInput "command.name", "", "password"/>

FORM14
<@infra.formSingleSelect "command.name", options, ""/>

FORM15
<@infra.formCheckbox "command.name"/>

FORM16
<@infra.formCheckbox "command.jedi"/>

FORM17
<@infra.formInput "command.spouses[0].name", ""/>

FORM18
<@infra.formCheckbox "command.spouses[0].jedi" />
