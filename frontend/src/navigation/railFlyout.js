/**
 * Position a non-modal rail flyout in viewport coordinates. The flyout is
 * teleported to body so a scrollable rail cannot clip its contents.
 */
export function getRailFlyoutLayout(
  anchor,
  { viewportWidth, viewportHeight, itemCount } = {}
) {
  if (!anchor || viewportWidth <= 0 || viewportHeight <= 0) return null

  const edge = 12
  const left = Math.min(viewportWidth - edge, Math.max(edge, anchor.right + 10))
  const availableWidth = Math.max(0, viewportWidth - left - edge)
  // Two or three cards per row on a tablet, depending on remaining space.
  const desiredColumns = itemCount <= 2 ? 2 : 3
  const desiredWidth = desiredColumns * 166 + 32
  const width = Math.min(availableWidth, desiredWidth)
  const innerWidth = Math.max(0, width - 32)
  const columns = Math.max(1, Math.min(desiredColumns, Math.floor((innerWidth + 10) / 166)))
  const estimatedHeight = 72 + Math.ceil(itemCount / columns) * 60
  const viewportSpace = Math.max(0, viewportHeight - edge * 2)
  const visibleHeight = Math.min(viewportSpace, estimatedHeight)
  const top = Math.max(edge, Math.min(anchor.top - 8, viewportHeight - visibleHeight - edge))
  // A taller-than-estimated label can wrap; cap the actual panel below top.
  const maxHeight = Math.max(0, viewportHeight - top - edge)

  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    maxHeight: `${maxHeight}px`,
  }
}
