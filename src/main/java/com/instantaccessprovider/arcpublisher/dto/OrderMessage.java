// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.dto;

/**
 * Message de la file d'ordres (topic {@code arc.orders}), émis par les managers.
 * Hérite des champs d'une requête de création ; la clé Kafka doit être l'identifiant de l'ordre
 * pour que SUBMIT puis CANCEL d'un même ordre restent ordonnés (même partition).
 *
 * <ul>
 *   <li>{@code SUBMIT} (défaut) : planifie l'ordre ({@code id} optionnel mais recommandé).</li>
 *   <li>{@code CANCEL} : annule l'ordre {@code id} s'il est encore en attente ; seul {@code id} est requis.</li>
 * </ul>
 */
public class OrderMessage extends CreateOrderRequest {

    public enum Operation { SUBMIT, CANCEL }

    private Operation operation = Operation.SUBMIT;

    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) {
        this.operation = operation == null ? Operation.SUBMIT : operation;
    }
}
