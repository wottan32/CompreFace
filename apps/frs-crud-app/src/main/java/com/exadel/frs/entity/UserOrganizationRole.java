package com.exadel.frs.entity;

import com.exadel.frs.enums.OrganizationRole;
import com.exadel.frs.helpers.OrganizationRoleConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrganizationRole {

    @EmbeddedId
    private UserOrganizationRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("organizationId")
    private Organization organization;

    @Convert(converter = OrganizationRoleConverter.class)
    private OrganizationRole role;

    public UserOrganizationRole(User user, Organization organization, OrganizationRole role) {
        this.user = user;
        this.organization = organization;
        this.role = role;
        this.id = new UserOrganizationRoleId(user.getId(), organization.getId());
    }

}
