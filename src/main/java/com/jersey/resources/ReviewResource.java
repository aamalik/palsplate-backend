package com.jersey.resources;

import com.jersey.persistence.CustomerDao;
import com.jersey.persistence.PersonDao;
import com.jersey.persistence.ReviewDao;
import com.jersey.representations.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("")
@Component
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReviewResource {

    private static final Logger log = LogManager.getLogger(ReviewResource.class);

    private final ReviewDao reviewDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private PersonDao personDao;

    @Inject
    public ReviewResource(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    @GET
    @Path("public/reviews")
    public List<Review> getAll(@QueryParam("page") Integer page,
                               @QueryParam("size") Integer size,
                               @QueryParam("sort") List<String> sort) {

        if(page == null && size == null)
        {
            return this.reviewDao.findAll();
        }

        //set default value for page
        if(page == null)
        {
            page = new Integer(0);
        }

        //set defaullt value for size
        if(size == null)
        {
            size = new Integer(3);
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (String propOrder: sort) {

            String[] propOrderSplit = propOrder.split(",");
            String property = propOrderSplit[0];

            if (propOrderSplit.length == 1) {
                orders.add(new Sort.Order(property));
            } else {
                Sort.Direction direction
                        = Sort.Direction.fromStringOrNull(propOrderSplit[1]);
                orders.add(new Sort.Order(direction, property));
            }
        }

        Pageable pageable = new PageRequest(page, size, orders.isEmpty() ? null : new Sort(orders));

        return  this.reviewDao.findAll(pageable).getContent();
    }

    @GET
    @Path("public/reviews/{id}")
    public Review getCook(@PathParam("id")long id) {
        Review review = reviewDao.findOne(id);
        if (review == null) {
            throw new WebApplicationException((Response.Status.NOT_FOUND));
        }
        return review;
    }

    /**
     * Create new Review
     * @param review
     * @return new review
     */
    @POST
    @Path("secure/reviews")
    public Review save(@Valid Review review ) {
        return reviewDao.save(review);
    }


    /**
     * Update existing Review
     * @param id
     * @param review
     * @return updated review
     */
    @PUT
    @Path("secure/reviews/{id}")
//    @PreAuthorize("hasPermission(#id, 'ReviewResource', 'ROLE_USER,ROLE_ADMIN')")
    public Review update(@PathParam("id")long id, @Valid Review review) {
        if(reviewDao.findOne(id) == null){
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }else {
            review.setId(id);
            return reviewDao.save(review);
        }
    }

    /**
     * Delete review
     * @param id
     */
    @DELETE
    @Path("secure/reviews/{id}")
//    @PreAuthorize("hasPermission(#id, 'ReviewResource', 'ROLE_ADMIN')")
    public void delete(@PathParam("id")long id) {
        Review review = reviewDao.findOne(id);
        if(review == null){
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }else {
            reviewDao.delete(review);
        }
    }


    /**
     * Get food object with cooks information
     * @param
     * @return JSONObject
     */
    @GET
    @Path("public/reviews/{id}/customerinfo")
    public JSONObject getFoodsWithCooks(@PathParam("id")long id) {

        Review review = reviewDao.findOne(id);
        if (review == null) {
            throw new WebApplicationException((Response.Status.NOT_FOUND));
        }
        Customer customer= customerDao.findOne(review.getCustomer_id());
        Person person = personDao.findOne(customer.getPerson_id());

        JSONObject reviewCustomerinfo = new JSONObject();
        reviewCustomerinfo.put("id", review.getId());
        reviewCustomerinfo.put("customer_id", customer.getId());
        reviewCustomerinfo.put("customer_name", person.getFirstName() + " " + person.getLastName());
        reviewCustomerinfo.put("customer_photo_id", person.getPhotoPublicId());

        return reviewCustomerinfo;
    }
}