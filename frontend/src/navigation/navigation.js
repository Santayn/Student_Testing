import {
  BREADCRUMB_CONFIG,
  BREADCRUMB_ENTITIES,
  NAVIGATION_DESTINATIONS,
  WORKSPACE_NAVIGATION,
} from './navigation.config'

function cloneRouteQuery(query, keys = []) {
  const result = {}

  for (const key of keys) {
    const value = query?.[key]

    if (
      value !== undefined &&
      value !== null &&
      value !== ''
    ) {
      result[key] = value
    }
  }

  return result
}

function contextValue(route, context, key) {
  const contextValueResult = context?.[key]

  if (
    contextValueResult !== undefined &&
    contextValueResult !== null &&
    contextValueResult !== ''
  ) {
    return contextValueResult
  }

  const paramValue = route?.params?.[key]

  if (
    paramValue !== undefined &&
    paramValue !== null &&
    paramValue !== ''
  ) {
    return paramValue
  }

  const queryValue = route?.query?.[key]

  if (
    queryValue !== undefined &&
    queryValue !== null &&
    queryValue !== ''
  ) {
    return queryValue
  }

  return null
}

function entityId(route, context, entity) {
  const definition =
    BREADCRUMB_ENTITIES[entity]

  if (!definition) {
    return null
  }

  const fromContext = contextValue(
    route,
    context,
    definition.contextId
  )

  if (fromContext !== null) {
    return fromContext
  }

  const fromParam =
    definition.routeParam
      ? route?.params?.[
          definition.routeParam
        ]
      : null

  if (
    fromParam !== undefined &&
    fromParam !== null &&
    fromParam !== ''
  ) {
    return fromParam
  }

  const fromQuery =
    definition.routeQuery
      ? route?.query?.[
          definition.routeQuery
        ]
      : null

  if (
    fromQuery !== undefined &&
    fromQuery !== null &&
    fromQuery !== ''
  ) {
    return fromQuery
  }

  return null
}

function resolveEntityCrumb(
  descriptor,
  route,
  context
) {
  const definition =
    BREADCRUMB_ENTITIES[
      descriptor.entity
    ]

  if (!definition) {
    return null
  }

  const id = entityId(
    route,
    context,
    descriptor.entity
  )

  if (
    descriptor.optional &&
    (id === null || id === '')
  ) {
    return null
  }

  const contextLabel =
    context?.[
      definition.contextLabel
    ]

  const label =
    contextLabel ||
    (id !== null && id !== ''
      ? `${definition.fallbackLabel} #${id}`
      : definition.fallbackLabel)

  const params = {}

  if (
    definition.routeParamName &&
    id !== null &&
    id !== ''
  ) {
    params[
      definition.routeParamName
    ] = id
  }

  const canBuildRoute =
    Boolean(
      definition.routeName &&
      (!definition.routeParamName ||
        (id !== null && id !== ''))
    )

  return {
    key:
      descriptor.key ||
      definition.key,
    label,
    to: canBuildRoute
      ? {
          name:
            definition.routeName,
          ...(Object.keys(params).length
            ? { params }
            : {}),
          ...(descriptor.preserveQuery
            ?.length
            ? {
                query: cloneRouteQuery(
                  route?.query,
                  descriptor.preserveQuery
                ),
              }
            : {}),
        }
      : null,
  }
}

function resolveStaticCrumb(
  descriptor,
  route,
  context
) {
  if (descriptor.destination) {
    const destination =
      NAVIGATION_DESTINATIONS[
        descriptor.destination
      ]

    if (!destination) {
      return null
    }

    return {
      key: destination.key,
      label:
        descriptor.label ||
        destination.label,
      to: {
        name: destination.routeName,
        ...(descriptor.preserveQuery
          ?.length
          ? {
              query: cloneRouteQuery(
                route?.query,
                descriptor.preserveQuery
              ),
            }
          : {}),
      },
    }
  }

  const requiredValues =
    descriptor.requires?.map(
      (key) =>
        contextValue(
          route,
          context,
          key
        )
    ) ?? []

  if (
    requiredValues.some(
      (value) =>
        value === null ||
        value === ''
    )
  ) {
    return descriptor.optional
      ? null
      : {
          key: descriptor.key,
          label: descriptor.label,
          to: null,
        }
  }

  const params = {}

  for (
    const key of
    descriptor.paramsFrom ?? []
  ) {
    const value = contextValue(
      route,
      context,
      key
    )

    if (
      value !== null &&
      value !== ''
    ) {
      params[key] = value
    }
  }

  return {
    key: descriptor.key,
    label: descriptor.label,
    to: descriptor.routeName
      ? {
          name: descriptor.routeName,
          ...(Object.keys(params).length
            ? { params }
            : {}),
          ...(descriptor.preserveQuery
            ?.length
            ? {
                query: cloneRouteQuery(
                  route?.query,
                  descriptor.preserveQuery
                ),
              }
            : {}),
        }
      : null,
  }
}

function resolveWorkspaceItem(item) {
  const descriptor =
    typeof item === 'string'
      ? { key: item }
      : item

  const destination =
    NAVIGATION_DESTINATIONS[
      descriptor.key
    ]

  if (!destination) {
    return null
  }

  return {
    ...destination,
    label:
      descriptor.label ??
      destination.label,
    route: {
      name: destination.routeName,
    },
  }
}

export function getWorkspaceNavigation(
  workspaceRole
) {
  const sections =
    WORKSPACE_NAVIGATION[
      workspaceRole
    ] ?? []

  return sections.map(
    (section) => ({
      key: section.key,
      label: section.label,
      kind:
        section.kind ??
        'navigation',
      items: section.items
        .map(resolveWorkspaceItem)
        .filter(Boolean),
    })
  )
}

export function getActiveNavigationKey(
  route
) {
  return route?.meta?.navKey ?? null
}

export function resolveBreadcrumbs(
  route,
  context = {}
) {
  const breadcrumbKey =
    route?.meta?.breadcrumbKey ??
    route?.name

  if (!breadcrumbKey) {
    return []
  }

  const descriptors =
    BREADCRUMB_CONFIG[
      breadcrumbKey
    ] ?? []

  const crumbs = descriptors
    .map((descriptor) => {
      if (descriptor.entity) {
        return resolveEntityCrumb(
          descriptor,
          route,
          context
        )
      }

      return resolveStaticCrumb(
        descriptor,
        route,
        context
      )
    })
    .filter(Boolean)

  return crumbs.map(
    (crumb, index) => ({
      ...crumb,
      current:
        index ===
        crumbs.length - 1,
      to:
        index ===
        crumbs.length - 1
          ? null
          : crumb.to,
    })
  )
}
