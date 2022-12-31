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
package eu.lundegaard.liferay.db.setup.core;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.service.FragmentCollectionLocalServiceUtil;
import com.liferay.fragment.service.FragmentEntryLinkLocalServiceUtil;
import com.liferay.fragment.service.FragmentEntryLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import eu.lundegaard.liferay.db.setup.core.util.ResourcesUtil;
import eu.lundegaard.liferay.db.setup.domain.Fragment;
import eu.lundegaard.liferay.db.setup.domain.FragmentCollection;
import eu.lundegaard.liferay.db.setup.domain.FragmentData;
import static eu.lundegaard.liferay.db.setup.domain.SetupActionType.CREATE;

/**
 * @author michal.volf@lundegaard.eu
 * @author jakun.jandak@lundegaard.eu 2022
 */
public class SetupFragments {

    private static final Log LOG = LogFactoryUtil.getLog(SetupFragments.class);

    private SetupFragments() {}

    public static void setupFragments(List<FragmentCollection> fragmentCollections, long userId, long groupId) {
        for (FragmentCollection fragmentCollection : fragmentCollections) {
            ServiceContext serviceContext = new ServiceContext();

            if (fragmentCollection.getSetupAction() == null) {
                fragmentCollection.setSetupAction(CREATE);
            }

            switch (fragmentCollection.getSetupAction()) {
                case CREATE:
                case UPDATE:
                    saveFragmentCollection(fragmentCollection, userId, groupId, serviceContext);
                    break;
                case DELETE:
                    deleteFragmentCollection(fragmentCollection, groupId);
                    break;
                default:
                    throw new IllegalArgumentException("Illegal setup action " + fragmentCollection.getSetupAction());
            }

            LOG.info("Setup for fragment collection " + fragmentCollection.getName() + " finished");
        }
    }

    private static void saveFragmentCollection(FragmentCollection fragmentCollection, long userId, long groupId,
            ServiceContext serviceContext) {
        String collectionName = fragmentCollection.getName();
        com.liferay.fragment.model.FragmentCollection collection;
        try {
            Optional<com.liferay.fragment.model.FragmentCollection> existingCollection =
                    findFragmentCollection(collectionName, groupId);

            if (existingCollection.isPresent()) {
                LOG.info("Updating collection " + fragmentCollection.getName());
                collection = FragmentCollectionLocalServiceUtil.updateFragmentCollection(
                        existingCollection.get().getFragmentCollectionId(), fragmentCollection.getName(),
                        fragmentCollection.getDescription());
            } else {
                LOG.info("Creating collection " + fragmentCollection.getName());
                collection = FragmentCollectionLocalServiceUtil.addFragmentCollection(
                        userId, groupId, collectionName, fragmentCollection.getDescription(),
                        serviceContext);
            }
            for (Fragment fragment : fragmentCollection.getFragment()) {

                if (fragment.getSetupAction() == null) {
                    fragment.setSetupAction(CREATE);
                }

                switch (fragment.getSetupAction()) {
                    case CREATE:
                    case UPDATE:
                        saveFragment(fragment, userId, groupId, collection, serviceContext);
                        break;
                    case DELETE:
                        deleteFragment(fragment, groupId, collection);
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Illegal setup action " + fragmentCollection.getSetupAction());
                }
            }

        } catch (PortalException e) {
            LOG.error("Error during setup of collection " + collectionName, e);
        }
    }

    private static void deleteFragmentCollection(FragmentCollection fragmentCollection, long groupId) {
        String collectionName = fragmentCollection.getName();
        LOG.info("Trying to delete fragment collection " + collectionName);

        Optional<com.liferay.fragment.model.FragmentCollection> existingCollection =
                findFragmentCollection(collectionName, groupId);
        if (existingCollection.isPresent()) {
            LOG.info("Fragment collection " + collectionName + " found, deleting...");
            try {
                com.liferay.fragment.model.FragmentCollection collection = existingCollection.get();
                if (FragmentEntryLocalServiceUtil.getFragmentEntriesCount(collection.getFragmentCollectionId()) > 0) {
                    LOG.info("Fragment collection " + collectionName + " has fragments, deleting them first...");
                    deleteChildFragments(collection, groupId);
                }

                FragmentCollectionLocalServiceUtil
                        .deleteFragmentCollection(collection.getFragmentCollectionId());
                LOG.info("Collection deleted successfully");
            } catch (PortalException e) {
                LOG.error("Error during deleting the collection " + collectionName, e);
            }
        } else {
            LOG.warn("Collection " + collectionName + " not found");
        }
    }

    private static Optional<com.liferay.fragment.model.FragmentCollection> findFragmentCollection(String collectionName,
            long groupId) {
        return FragmentCollectionLocalServiceUtil
                .getFragmentCollections(groupId, 0,
                        FragmentCollectionLocalServiceUtil.getFragmentCollectionsCount())
                .stream()
                .filter(collection -> collection.getName().equals(collectionName))
                .findFirst();
    }

    private static void saveFragment(Fragment fragment, long userId, long groupId,
            com.liferay.fragment.model.FragmentCollection createdCollection, ServiceContext serviceContext) {
        try {
            String html = getContentFromElement(fragment.getHtml(), fragment.getName());
            String css = getContentFromElement(fragment.getCss(), fragment.getName());
            String js = getContentFromElement(fragment.getJs(), fragment.getName());
            String config = getContentFromElement(fragment.getConfiguration(), fragment.getName());

            Optional<FragmentEntry> existingFragment = findFragment(fragment, createdCollection, groupId);

            long fragmentCollectionId = 0;
            long previewFileEntryId = 0;
            boolean cachable = true;

            if (existingFragment.isPresent()) {
                LOG.info("Updating fragment " + fragment.getName());
                FragmentEntryLocalServiceUtil.updateFragmentEntry(userId, existingFragment.get().getFragmentEntryId(),
                        fragmentCollectionId, fragment.getName(), css, html, js, cachable,
                        config, "icon", previewFileEntryId, 0);
            } else {
                LOG.info("Creating fragment " + fragment.getName());
                FragmentEntryLocalServiceUtil.addFragmentEntry(userId, groupId,
                        createdCollection.getFragmentCollectionId(), fragment.getEntryKey(), fragment.getName(),
                        css, html, js, cachable, config, "icon",
                        previewFileEntryId, 1, "typeOptions", 0, serviceContext);
            }
        } catch (PortalException e) {
            LOG.error("Error during setup of fragment " + fragment.getName(), e);
        }

    }

    private static String getContentFromElement(FragmentData fragmentData, String fragmentName) {
        String content = "";
        try {
            if (fragmentData.getPath() != null && !fragmentData.getPath().isEmpty()) {
                content = ResourcesUtil.getFileContent(fragmentData.getPath());
            } else {
                content = fragmentData.getValue();
            }

        } catch (IOException e) {
            LOG.error("Error during setup of fragment " + fragmentName, e);
        }
        return content;
    }

    private static void deleteFragment(Fragment fragment, long groupId,
            com.liferay.fragment.model.FragmentCollection createdCollection) {
        String fragmentName = fragment.getName();
        LOG.info("Deleting fragment " + fragmentName);

        Optional<FragmentEntry> existingFragment = findFragment(fragment, createdCollection, groupId);
        if (existingFragment.isPresent()) {
            LOG.info("Fragment " + fragmentName + " found, deleting...");
            try {
                FragmentEntry fragmentEntry = existingFragment.get();
                if (FragmentEntryLinkLocalServiceUtil.getAllFragmentEntryLinksCountByFragmentEntryId(groupId,
                        fragmentEntry.getFragmentEntryId()) > 0) {
                    LOG.warn("Fragment " + fragmentName + " has usages, can not be deleted");
                } else {
                    FragmentEntryLocalServiceUtil
                            .deleteFragmentEntry(fragmentEntry.getFragmentEntryId());
                    LOG.info("Fragment deleted successfully");
                }
            } catch (PortalException e) {
                LOG.error("Error during deleting the fragment " + fragmentName, e);
            }
        } else {
            LOG.warn("Fragment " + fragmentName + " not found");
        }
    }

    private static Optional<FragmentEntry> findFragment(Fragment fragment,
            com.liferay.fragment.model.FragmentCollection createdCollection, long groupId) {
        return FragmentEntryLocalServiceUtil
                .getFragmentEntries(groupId, createdCollection.getFragmentCollectionId(), 0).stream()
                .filter(fragmentEntry -> fragmentEntry.getFragmentEntryKey()
                        .equals(fragment.getEntryKey()))
                .findFirst();
    }

    private static void deleteChildFragments(com.liferay.fragment.model.FragmentCollection collection, long groupId)
            throws PortalException {
        for (FragmentEntry fragmentEntry : FragmentEntryLocalServiceUtil
                .getFragmentEntries(collection.getFragmentCollectionId())) {
            if (FragmentEntryLinkLocalServiceUtil.getAllFragmentEntryLinksCountByFragmentEntryId(groupId,
                    fragmentEntry.getFragmentEntryId()) > 0) {
                LOG.warn("Fragment " + fragmentEntry.getName() + " has usages, can not be deleted");
            } else {
                FragmentEntryLocalServiceUtil
                        .deleteFragmentEntry(fragmentEntry.getFragmentEntryId());
            }
        }
    }

}
