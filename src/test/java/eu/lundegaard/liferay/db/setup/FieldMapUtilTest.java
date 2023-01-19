/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Lundegaard a.s.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.lundegaard.liferay.db.setup;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import eu.lundegaard.liferay.db.setup.core.util.FieldMapUtil;
import eu.lundegaard.liferay.db.setup.core.support.PortalUtilFacade;
import eu.lundegaard.liferay.db.setup.domain.DescriptionTranslation;
import eu.lundegaard.liferay.db.setup.domain.TitleTranslation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class FieldMapUtilTest {

    @Test
    void getTitleMap() {
        TitleTranslation titleTranslationCs = new TitleTranslation();
        titleTranslationCs.setLocale("cs_CZ");
        titleTranslationCs.setTitleText("Nadpis");
        TitleTranslation titleTranslationEn = new TitleTranslation();
        titleTranslationEn.setLocale("en_US");
        titleTranslationEn.setTitleText("Title");
        try (MockedStatic<PortalUtilFacade> fieldMapUtilMockedStatic = Mockito.mockStatic(PortalUtilFacade.class)) {
            fieldMapUtilMockedStatic.when(() -> PortalUtilFacade.getDefaultLocale(0L, "Čeština"))
                    .thenReturn(new Locale("cs_CZ"));
            Map<Locale, String> titleMap =
                    FieldMapUtil.getTitleMap(Arrays.asList(titleTranslationCs, titleTranslationEn), 0L,
                            "Defaultní nadpis",
                            "Čeština");

            Assertions.assertEquals(3, titleMap.size()); // 1 Default, 1 English, 1 Czech
        }
    }

    @Test
    void getDescriptionMap() {
        DescriptionTranslation descriptionTranslation = new DescriptionTranslation();
        descriptionTranslation.setLocale("cs_CZ");
        descriptionTranslation.setTitleText("Popis");
        DescriptionTranslation descriptionTranslationEn = new DescriptionTranslation();
        descriptionTranslationEn.setLocale("en_US");
        descriptionTranslationEn.setTitleText("Description");
        try (MockedStatic<PortalUtilFacade> fieldMapUtilMockedStatic = Mockito.mockStatic(PortalUtilFacade.class)) {
            fieldMapUtilMockedStatic.when(() -> PortalUtilFacade.getDefaultLocale(0L, "Čeština"))
                    .thenReturn(new Locale("cs_CZ"));
            Map<Locale, String> descriptionMap =
                    FieldMapUtil.getDescriptionMap(Arrays.asList(descriptionTranslation, descriptionTranslationEn), 0L,
                            "Defaultní popis",
                            "Čeština");

            Assertions.assertEquals(3, descriptionMap.size()); // 1 Default, 1 English, 1 Czech
        }
    }
}
