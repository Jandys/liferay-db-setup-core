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

import java.io.File;
import com.liferay.journal.model.JournalArticle;
import eu.lundegaard.liferay.db.setup.core.support.ClassNameLocalServiceUtilWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jakub Jandák, jakub.jandak@lundegaard.eu, 2023
 */

public class MockTestCore {

    protected static File invalidConfiguration = new File("src/test/resources/schemas/invalidschema.xml");
    protected static File validConfiguration = new File("src/test/resources/schemas/validschema.xml");

    protected MockedStatic<ClassNameLocalServiceUtilWrapper> classNameLocalServiceUtil;

    @BeforeEach
    public void setup() {
        classNameLocalServiceUtil = Mockito.mockStatic(ClassNameLocalServiceUtilWrapper.class);
        classNameLocalServiceUtil.when(() -> ClassNameLocalServiceUtilWrapper.getClassNameId(JournalArticle.class))
                .thenReturn(0L);
    }



}
