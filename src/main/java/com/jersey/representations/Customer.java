package com.jersey.representations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

@JsonIgnoreProperties
@Entity
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "createdat", updatable = false)
    @CreationTimestamp
    private Date createdat;

    @Column(name = "updatedat")
    @UpdateTimestamp
    private Date updatedat;

    @JoinColumn(name = "person_id")
    @NotNull
    private Long person_id;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Set<Reservation> reservations;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="customer_id")
    private Set<Review> reviews;

    public Customer() {
    }

    public Customer(Long id, String name, String currency, Double regularPrice, Double discountPrice) {
        this.id = id;
    }

    public Long getId() { return id; }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPerson_id() {
        return person_id;
    }

    public Date getCreatedat() { return createdat; }

    public void setCreatedat(Date createdat) { this.createdat = createdat; }

    public Date getUpdatedat() {
        return updatedat;
    }

    public void setUpdatedat(Date updatedat) {
        this.updatedat = updatedat;
    }

    public void setPerson_id(Long person_id) {
        this.person_id = person_id;
    }

    public Set<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(Set<Reservation> reservations) {
        this.reservations = reservations;
    }

    public Set<Review> getReviews() {
        return reviews;
    }

    public void setReviews(Set<Review> reviews) {
        this.reviews = reviews;
    }

}
