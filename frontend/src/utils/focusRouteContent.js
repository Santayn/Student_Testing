export function focusRouteContent(mainElement) {
  if (!mainElement) {
    return null
  }

  const heading =
    mainElement.querySelector('h1')

  const focusTarget =
    heading ?? mainElement

  if (
    heading &&
    !heading.hasAttribute('tabindex')
  ) {
    heading.setAttribute(
      'tabindex',
      '-1'
    )
  }

  focusTarget.focus({
    preventScroll: true,
  })

  return focusTarget
}
