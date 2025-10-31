package com.androidvoiceapp.di

import javax.inject.Qualifier

/**
 * Qualifier to distinguish Mock API implementations
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MockApi

/**
 * Qualifier to distinguish Production API implementations
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProductionApi
