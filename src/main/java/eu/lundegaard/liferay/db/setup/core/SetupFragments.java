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

    private static final String REQUIRED_FRAGMENT_CONFIGURATION = "{\"fieldSets\":[]}";
    private static final Log LOG = LogFactoryUtil.getLog(SetupFragments.class);
    private static final String FRAGMENT = "Fragment ";

    private SetupFragments() {}

    /**
     * Sets up the fragments based on the list of {@link FragmentCollection} objects
     * provided. The setup action for each fragment collection can be set as CREATE,
     * UPDATE, or DELETE, and the appropriate action is taken.
     *
     * @param fragmentCollections The list of {@link FragmentCollection} objects.
     * @param userId The ID of the user who created the collection.
     * @param groupId The ID of the group to which the collection belongs.
     * @param companyId The ID of the company.
     * @throws IllegalArgumentException if the setup action is set to anything other
     *         than CREATE, UPDATE, or DELETE.
     */
    public static void setupFragments(List<FragmentCollection> fragmentCollections, long userId, long groupId,
            long companyId) {
        for (FragmentCollection fragmentCollection : fragmentCollections) {
            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setCompanyId(companyId);

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

    /**
     * Saves a {@link FragmentCollection} object to the database. The method checks
     * if the collection already exists in the database, and if it does, it updates
     * the existing record. If not, it creates a new record. The method also saves
     * all the fragments associated with the collection. The setup action for each
     * fragment can be set as CREATE, UPDATE, or DELETE, and the appropriate action
     * is taken.
     *
     * @param fragmentCollection The {@link FragmentCollection} object to be saved.
     * @param userId The ID of the user who created the collection.
     * @param groupId The ID of the group to which the collection belongs.
     * @param serviceContext The {@link ServiceContext} object.
     * @throws IllegalArgumentException if the setup action is set to anything other
     *         than CREATE, UPDATE, or DELETE.
     * @throws PortalException if there is an error during the setup of the
     *         collection.
     */
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

    /**
     * Deletes a {@link FragmentCollection} with the specified name and group ID.
     *
     * @param fragmentCollection The fragment collection to be deleted
     * @param groupId The group ID of the fragment collection
     */
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

    /**
     * Finds the {@link com.liferay.fragment.model.FragmentCollection} with the
     * given name and group ID.
     *
     * @param collectionName the name of the FragmentCollection to be found
     * @param groupId the group ID of the FragmentCollection to be found
     * @return an {@link Optional} containing the found FragmentCollection, or empty
     *         if not found
     */
    private static Optional<com.liferay.fragment.model.FragmentCollection> findFragmentCollection(String collectionName,
            long groupId) {
        return FragmentCollectionLocalServiceUtil
                .getFragmentCollections(groupId, 0,
                        FragmentCollectionLocalServiceUtil.getFragmentCollectionsCount())
                .stream()
                .filter(collection -> collection.getName().equals(collectionName))
                .findFirst();
    }

    /**
     * Saves a fragment in Liferay.
     *
     * @param fragment the fragment to be saved
     * @param userId the ID of the user who is saving the fragment
     * @param groupId the ID of the group to which the fragment belongs
     * @param collection the fragment collection in which the fragment will be saved
     * @param serviceContext the service context for the operation
     * @throws PortalException if there is an error during the setup of the fragment
     */
    private static void saveFragment(Fragment fragment, long userId, long groupId,
            com.liferay.fragment.model.FragmentCollection collection, ServiceContext serviceContext) {
        try {
            String html = getContentFromElement(fragment.getHtml(), fragment.getName());
            String css = getContentFromElement(fragment.getCss(), fragment.getName());
            String js = getContentFromElement(fragment.getJs(), fragment.getName());
            String config = getContentFromElement(fragment.getConfiguration(), fragment.getName());

            if (config.isEmpty() || (!config.contains("{") || !config.contains("}"))) {
                config = REQUIRED_FRAGMENT_CONFIGURATION;
            }

            Optional<FragmentEntry> existingFragment = findFragment(fragment, collection, groupId);

            long previewFileEntryId = 0;
            boolean cachable = true;
            String icon = null;
            String typeOptions = null;

            if (existingFragment.isPresent()) {
                LOG.info("Updating fragment " + fragment.getName());
                FragmentEntryLocalServiceUtil.updateFragmentEntry(userId, existingFragment.get().getFragmentEntryId(),
                        collection.getFragmentCollectionId(), fragment.getName(), css, html, js, cachable,
                        config, icon, previewFileEntryId, 0);
            } else {
                LOG.info("Creating fragment " + fragment.getName());
                FragmentEntryLocalServiceUtil.addFragmentEntry(userId, groupId,
                        collection.getFragmentCollectionId(), fragment.getEntryKey(), fragment.getName(),
                        css, html, js, cachable, config, icon,
                        previewFileEntryId, 1, typeOptions, 0, serviceContext);
            }
        } catch (PortalException e) {
            LOG.error("Error during setup of fragment " + fragment.getName(), e);
        }

    }

    /**
     * Retrieves the content of a fragment data element.
     *
     * @param fragmentData the fragment data element to retrieve the content from
     * @param fragmentName the name of the fragment
     * @return the content of the fragment data element
     */
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

    /**
     * Deletes a fragment.
     *
     * @param fragment the fragment to delete
     * @param groupId the group ID
     * @param createdCollection the created fragment collection
     */
    private static void deleteFragment(Fragment fragment, long groupId,
            com.liferay.fragment.model.FragmentCollection createdCollection) {
        String fragmentName = fragment.getName();
        LOG.info("Deleting fragment " + fragmentName);

        Optional<FragmentEntry> existingFragment = findFragment(fragment, createdCollection, groupId);
        if (existingFragment.isPresent()) {
            LOG.info(FRAGMENT + fragmentName + " found, deleting...");
            try {
                FragmentEntry fragmentEntry = existingFragment.get();
                if (FragmentEntryLinkLocalServiceUtil.getAllFragmentEntryLinksCountByFragmentEntryId(groupId,
                        fragmentEntry.getFragmentEntryId()) > 0) {
                    LOG.warn(FRAGMENT + fragmentName + " has usages, can not be deleted");
                } else {
                    FragmentEntryLocalServiceUtil
                            .deleteFragmentEntry(fragmentEntry.getFragmentEntryId());
                    LOG.info("Fragment deleted successfully");
                }
            } catch (PortalException e) {
                LOG.error("Error during deleting the fragment " + fragmentName, e);
            }
        } else {
            LOG.warn(FRAGMENT + fragmentName + " not found");
        }
    }

    /**
     * Finds a fragment by key.
     *
     * @param fragment the fragment to find
     * @param createdCollection the created fragment collection
     * @param groupId the group ID
     * @return an optional object containing the found fragment, or an empty
     *         optional if no such fragment is found
     */
    private static Optional<FragmentEntry> findFragment(Fragment fragment,
            com.liferay.fragment.model.FragmentCollection createdCollection, long groupId) {
        return FragmentEntryLocalServiceUtil
                .getFragmentEntries(groupId, createdCollection.getFragmentCollectionId(), 0).stream()
                .filter(fragmentEntry -> fragmentEntry.getFragmentEntryKey()
                        .equals(fragment.getEntryKey()))
                .findFirst();
    }

    /**
     * Deletes all child fragments of a given fragment collection.
     *
     * @param collection The fragment collection whose child fragments should be
     *        deleted
     * @param groupId The group ID of the fragment collection
     * @throws PortalException if an error occurs during the deletion process
     */
    private static void deleteChildFragments(com.liferay.fragment.model.FragmentCollection collection, long groupId)
            throws PortalException {
        for (FragmentEntry fragmentEntry : FragmentEntryLocalServiceUtil
                .getFragmentEntries(collection.getFragmentCollectionId())) {
            if (FragmentEntryLinkLocalServiceUtil.getAllFragmentEntryLinksCountByFragmentEntryId(groupId,
                    fragmentEntry.getFragmentEntryId()) > 0) {
                LOG.warn(FRAGMENT + fragmentEntry.getName() + " has usages, can not be deleted");
            } else {
                FragmentEntryLocalServiceUtil
                        .deleteFragmentEntry(fragmentEntry.getFragmentEntryId());
            }
        }
    }

}
