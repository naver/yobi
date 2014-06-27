/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @Author Yi EungJun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models;

import models.enumeration.ResourceType;
import models.resource.GlobalResource;
import models.resource.Resource;
import models.resource.ResourceConvertible;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Set;

/**
 * A label for projects
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"category", "name"}))
public class Label extends Model implements ResourceConvertible {

    private static final long serialVersionUID = -35487506476718498L;
    public static final Finder<Long, Label> find = new Finder<>(Long.class, Label.class);

    @Id
    public Long id;

    @Required
    public String category;

    @Required
    public String name;

    @ManyToMany(mappedBy="labels")
    public Set<Project> projects;

    /**
     * Constructs a label with the given {@code name} and {@code category}.
     *
     * @param category  category to which this label belongs
     * @param name      name of this label
     */
    public Label(String category, String name) {
        if (category == null) {
            category = "Label";
        }
        this.category = category;
        this.name = name;
    }

    /**
     * Deletes a label.
     *
     * Removes a label from every project and then delete it.
     */
    @Override
    public void delete() {
        for(Project project: projects) {
            project.labels.remove(this);
            project.update();
        }
        super.delete();
    }


    /**
     * Returns a string representation of this label.
     *
     * @return string concatenated {@link Label#category}, "-" and {@link Label#name}
     *         e.g. "os - linux"
     */
    @Override
    public String toString() {
        return category + " - " + name;
    }

    /**
     * Returns {@link Resource} representation of this label.
     *
     * {@link utils.AccessControl}.may use this method to check if a user has
     * a permission to access this label.
     *
     * @return {@link Resource} representation of this label
     */
    @Override
    public Resource asResource() {
        return new GlobalResource() {
            @Override
            public String getId() {
                return id.toString();
            }

            @Override
            public ResourceType getType() {
                return ResourceType.LABEL;
            }
        };
    }

    /**
     * Removes this label from a project.
     *
     * @param project the project from which this label is removed
     */
    public void delete(Project project) {
        this.projects.remove(project);
    }
}
