/**
 * Copyright (c) 2008-2010 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sakaiproject.profile2.tool.dataproviders;

import java.util.Iterator;

import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.profile2.logic.ProfileImageLogic;
import org.sakaiproject.profile2.model.GalleryImage;
import org.sakaiproject.profile2.tool.models.DetachableGalleryImageModel;

/**
 * IDataProvider implementation for retrieving gallery images.
 */
public class GalleryImageDataProvider implements IDataProvider<GalleryImage> {

	private static final long serialVersionUID = 1L;
	private String userId;
	
	@SpringBean(name="org.sakaiproject.profile2.logic.ProfileImageLogic")
	private ProfileImageLogic imageLogic;

	public GalleryImageDataProvider(String userId) {
		
		//inject
		InjectorHolder.getInjector().inject(this);
		
		this.userId = userId;
	}

	public Iterator<GalleryImage> iterator(int first, int count) {	
		return imageLogic.getGalleryImages(userId).subList(first, first + count).iterator();
	}

	public IModel<GalleryImage> model(GalleryImage object) {
		if (!(object instanceof GalleryImage)) {
			throw new IllegalArgumentException("object not an instance of GalleryImage");
		}

		return new DetachableGalleryImageModel(object);
	}

	public int size() {
		return imageLogic.getGalleryImages(userId).size();
	}

	public void detach() {

	}

}
