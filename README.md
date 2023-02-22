[![Artifact](https://maven-badges.herokuapp.com/maven-central/eu.lundegaard.liferay/liferay-db-setup-core/badge.svg?color=blue)](https://search.maven.org/search?q=g:eu.lundegaard.liferay%20AND%20a:liferay-db-setup-core) [![Javadocs](https://www.javadoc.io/badge/eu.lundegaard.liferay/liferay-db-setup-core.svg?color=blue)](https://www.javadoc.io/doc/eu.lundegaard.liferay/liferay-db-setup-core)

# Liferay Portal DB Setup core
Library that allows to setup a number of Liferay artifacts in a DB. It uses xml configuration and Liferay APIs to add all configured artifacts. Artifacts in the database are created by Liferay common **upgrade process**. Each step of the upgrade process consists of one or more xml files, in which you can define artifacts to create or update.

> **What's new in 4.0.0?**\
> Upgrade to work with Liferay API version 7.4.\
> Addition of possibility to setup artifacts for not default company.\
> Addition of Tags.

## Usage

First add this dependency into your OSGi module project's `pom.xml`.

```xml
<dependency>
    <groupId>eu.lundegaard.liferay</groupId>
    <artifactId>liferay-db-setup-core</artifactId>
    <version>3.2.0</version>
</dependency>
```

and specify the dependency in your `bnd.bnd` file as a resource to include.

```properties
Include-Resource: @liferay-db-setup-core-3.2.0.jar
```

Second create `UpgradeStepRegistrator` component to register your upgrade steps, e.g.

```java
@Component(immediate = true, service = UpgradeStepRegistrator.class)
public class MyPortalUpgrade implements UpgradeStepRegistrator {

    @Override
    public void register(Registry registry) {
        String packageName = MyPortalUpgrade.class.getPackage().getName();
        registry.register(packageName, "1.0.0", "1.0.1", new GenericUpgradeStep("v1_0_1"));
    }
    
}
```

You can also call one of the `LiferaySetup.setup` methods directly to setup the database.

### XML File content

XML file of an upgrade step has usually this structure:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<setup xmlns="http://www.lundegaard.eu/liferay/setup">
    <configuration>
        <runasuser>test@liferay.com</runasuser>
        <company>2050040</company>
    </configuration>

    <!-- Artifacts to manage --> 
</setup>
```

`runasuser` defines under which user artifacts will be created. Then you can specify as many artifacts to setup as you want.\
`runasuser` can be left blank (user is not filled) in this scenario, default companyID administrator will be used.

**NEW:**\
`company` defines in which company should the setups be done. It is _optional_ element.\
**Company** element can also have optional flag attributes
* useCompanyWebId
* useCompanyMx
* useVirtualHost
* useLogoId

Those determine what the content inside the element means.\
By Default _(without any additional attributes)_ content inside `company` element is read as **company ID**. 

--- 
### Example of whole XML file

For instance, this will create **Role** with Publisher as a name.

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<setup xmlns="http://www.lundegaard.eu/liferay/setup">
    <configuration>
        <runasuser>test@liferay.com</runasuser>
    </configuration>
 
    <roles>
        <role name="Publisher"/>
    </roles>
</setup>
```

---

## Features

In **Artifacts to manage** section you can specify a lot of artifacts.

### Roles

Code below creates **Role** named _"Content Admin"_ that can **VIEW** `com.liferay.layout.page.template.model.LayoutPageTemplateEntry`
```xml
<roles>
    <role name="Content admin">
        <define-permissions>
            <define-permission define-permission-name="com.liferay.layout.page.template.model.LayoutPageTemplateEntry">
                <permission-action action-name="VIEW"/>
            </define-permission>
        </define-permissions>
    </role>
</roles>
```

### Expando attribute

This will create expando attribute **canonical-url** with permissions to view by guest user.

```xml
<customFields>
    <field name="canonical-url" type="string" className="com.liferay.portal.kernel.model.Layout">
        <role-permission role-name="Guest">
            <permission-action action-name="VIEW"/>
        </role-permission>
    </field>
</customFields>
```

### Site

Site element must always have `site-friendly-url` filled. Guest site is determined by `default` attribute with `true` value. Other sites are specified by `name` attribute.

```xml
<sites>
    <site default="true" site-friendly-url="/guest">
    </site>
    <site name="My web" default="false" >
    </site>
</sites>
```

### Document Folder
Create **Document folder** named _"/Icons"_ if it does not exist.

```xml
<document-folder create-if-not-exists="true" folder-name="/Icons" />
```

### Document

Create **Document**.
Document's file itself is determined by `file-system-name` attribute which defines resource on classpath.

```xml
<document file-system-name="my-project/documents/icons/icon-home.svg"
          document-folder-name="/Icons"
          document-filename="icon-home.svg"
          document-title="icon-home.svg"/>
```
### Article Folder
Code below creates folder for articles.
```xml
<article-folder folder-path="/links" description="Folder for links"/>
```

### Articles

Article's content is determined by `path` attribute which defines resource on classpath. The resource contains article content in the form of XML.

```xml
<article title="Footer"
         path="my-project/articles/homepage/web_content/footer.xml"
         article-structure-key="BASIC-WEB-CONTENT"
         article-template-key="BASIC-WEB-CONTENT"
         articleId="FOOTER"
         article-folder-path="/Footer">
</article>
```

### Article Structure

```xml
<article-structure key="BANNER-MAIN"
                   path="my-project/articles/homepage/structures/banner-main.json"
                   name="Banner - main"/>
```

### Article Template

```xml
<article-template key="BANNER-MAIN"
                  path="my-project/articles/homepage/templates/banner-main.ftl"
                  article-structure-key="BANNER-MAIN" name="Banner - main" cacheable="true"/>
```

### Fragment Collection and Fragments

Fragment collections and fragments can be `created, updated or deleted`.
This action is determined via `setup-action` attributes.\
Fragment content can be set by `path` attribute or by inner tag `<![CDATA[]]>`.

```xml
 <fragment-collection name="FragmentCollection" setup-action="update">
        <fragment name="test-fragment" entryKey="test-fragmentKey">
            <html path="my-project/content/fragments/test-fragment/content.html" />
            <css>
            <![CDATA[
            .fragment-text-red{
            	color:red !important;
            }
            ]]>
            </css>
            <js></js>
            <configuration path="my-project/content/fragments/test-fragment/config.json" />
        </fragment>
    </fragment-collection>
```

### Organizations
Create Organization named `setup-organization`
```xml
<organizations>
   <organization name="setup-organization">
   </organization>
</organizations>
```

### User Groups
Create user group.
```xml
<userGroups>
   <userGroup name="setup-group" description="This is setup group.">
       <role name="Content administrator">
       </role>
   </userGroup>
</userGroups>
```

### User
Create User.
```xml
 <users>
    <user screenName="SetupUserOne" emailAddress="test@test.com" password="test" firstName="FirstName" lastName="LastName">
        <role name="Client administrator">
        </role>
    </user>
</users>
```

### Vocabulary and Categories
Creates **Vocabulary** and **Categories** within. 
```xml
<vocabulary name="setup-vocabulary">
    <category name="setup-category" description="This is created vocabulary.">
        <title-translation locale="en_US" title-text="Vocabulary"/>
        <title-translation locale="cs_CZ" title-text="Slovník"/>
    </category>
</vocabulary>
```

### Tags
Create **tags**.
```xml
<tags>
    <tag name="news-article"/>
    <tag name="faq-questions"/>
</tags>
```

### Pages 
Setup Public and Private Pages.
```xml
<public-pages>
    <page friendlyURL="/setup-page" name="Setup Page">
        <custom-field-setting key="IS_SUPERB_PAGE" value="true"/>
        <page friendlyURL="/child-page" name="Child Page">
            <title-translation locale="en_US" title-text="Child Page"/>
            <title-translation locale="cs_CZ" title-text="Podstránka"/>
        </page>
    </page>
</public-pages>
```
```xml
<private-pages>
    <page friendlyURL="/setup-private" name="Private Page">
    </page>
</private-pages>
```

### Others

You can create/update/set many other artifacts like Portlet placement, Permission, ... See source code.

## Compatibility

Liferay Portal Version | Version
---------------------- | -------
7.4.x | 4.x.x
7.3.x | 3.1.x
7.2.x | 3.0.x
older | use original [Mimacom library](https://github.com/mimacom/liferay-db-setup-core)

> **Note:** New changes introduced in version 4.0.0 may not be backwards compatible.
> Such as Tags and Company.\
> See the source if you are not sure about some features.
