/*
 * Copyright (C) 2018 Arthur Gregorio, AG.Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.com.webbudget.domain.repositories;

import br.com.webbudget.application.components.table.Page;
import br.com.webbudget.domain.entities.PersistentEntity;
import br.com.webbudget.domain.entities.PersistentEntity_;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.criteria.Criteria;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;

import javax.persistence.metamodel.SingularAttribute;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * The default implementation of a repository in the application
 * 
 * Every repository should extend this class to get some features that are not
 * default in the basic Deltaspike implementation
 *
 * @param <T> the type of
 * 
 * @author Arthur Gregorio
 *
 * @version 1.0.0
 * @since 3.0.0, 20/03/2018
 */
public interface DefaultRepository<T extends PersistentEntity> 
        extends EntityRepository<T, Long>, CriteriaSupport<T> {

    /**
     * Generic method to find a entity by Id
     * 
     * @param id the id to search
     * @return the entity in a optional state
     */
    Optional<T> findOptionalById(Long id);
    
    /**
     * Generic search method with lazy pagination support
     * 
     * To use this method you need to implement {@link #getRestrictions(java.lang.String)}
     * and {@link #getBlockedProperty()}
     * 
     * @param filter the string filter to use
     * @param blocked the object status of the entity, null means any states
     * @param start the start page
     * @param pageSize the size of the page
     * @return the list of objects found
     */
    default Page<T> findAllBy(String filter, Boolean blocked, int start, int pageSize) {
        
        final int totalRows = this.countPages(filter, blocked);
        
        final Criteria<T, T> criteria = criteria();
        
        if (isNotBlank(filter)) {
            criteria.or(this.getRestrictions(filter));
        } 
        
        if (blocked != null) {
            criteria.eq(this.getBlockedProperty(), blocked);
        }
                
        this.setOrder(criteria);
        
        final List<T> data = criteria.createQuery()
                .setFirstResult(start)
                .setMaxResults(pageSize)
                .getResultList();
        
        return Page.of(data, totalRows);
    }

    /**
     * Count the rows in the query to use it as pagecount on the pagination 
     * process
     * 
     * @param filter the filter to be applied on the count 
     * @param blocked blocked or not filter
     * @return the total pages
     */
    default int countPages(String filter, Boolean blocked) {
        
        final Criteria<T, T> criteria = criteria()
                .or(this.getRestrictions(filter));
        
        if (blocked != null) {
            criteria.eq(this.getBlockedProperty(), blocked);
        }
        
        return criteria
                .select(Long.class, count(PersistentEntity_.id))
                .getSingleResult()
                .intValue();
    }
    
    /**
     * Generic method to find all unblocked entities
     * 
     * @return a list of all unblocked entities
     */
    default List<T> findAllUnblocked() {
        
        final Criteria<T, T> criteria = criteria()
                .eq(this.getBlockedProperty(), false);

        this.setOrder(criteria);
                
        return criteria.getResultList();
    }
    
    /**
     * Use this method to set the default order to all the queries using the 
     * default repository
     * 
     * @param criteria the criteria to be used
     */
    default void setOrder(Criteria<T, T> criteria) {
        criteria.orderAsc(PersistentEntity_.id);
    }

    /**
     * This method shoud be implemented if the user needs to use the generic 
     * type search with the {@link #findAllBy(java.lang.String, java.lang.Boolean, int, int)}
     * method
     * 
     * With this we can detect all the restrictions to build the criteria 
     * 
     * @param filter the generic filter in {@link String} format
     * @return the criteria for the type of the repository
     */
    default Criteria<T, T> getRestrictions(String filter) {
        throw new RuntimeException("getRestrictions not implemented for query");
    }
    
    /**
     * This method shoud be implemented if the user needs to use the generic 
     * type search with the {@link #findAllBy(java.lang.String, java.lang.Boolean, int, int)}
     * method
     * 
     * @return the attribute responsible for representing the blocked property
     * of the entity
     */
    default SingularAttribute<T, Boolean> getBlockedProperty() {
        throw new RuntimeException("getBlockProperty not implemented for query");
    }
}
