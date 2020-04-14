

#include "NSArray+ArrayHelper.h"

@implementation NSArray (NSArrayHelper)

- (char**)getArray
{
    unsigned long count = [self count];
    char **array = (char **)malloc((count + 1) * sizeof(char*));

    for (unsigned i = 0; i < count; i++)
    {
         array[i] = strdup([[self objectAtIndex:i] UTF8String]);
    }
    array[count] = NULL;
    return array;
}

+ (void)freeArray:(char **)array
{
    if (array != NULL)
    {
        for (unsigned index = 0; array[index] != NULL; index++)
        {
            free(array[index]);
        }
        free(array);
    }
}

@end
