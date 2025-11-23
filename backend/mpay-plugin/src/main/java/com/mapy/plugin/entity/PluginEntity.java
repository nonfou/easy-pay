package com.mapy.plugin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plugin_definition")
@Getter
@Setter
public class PluginEntity {

    @Id
    @Column(nullable = false, length = 64)
    private String platform;

    @Column(nullable = false)
    private String name;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column
    private String price;

    @Column(name = "describe_text")
    private String describe;

    @Column
    private String website;

    @Column(nullable = false)
    private Integer state;

    @Column(nullable = false)
    private Boolean install;

    @Column(columnDefinition = "TEXT")
    private String query;
}
