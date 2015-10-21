using System;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Reflection;
using Microsoft.Win32;

using System.Xml;
using System.Xml.Serialization;
using System.Runtime.Serialization.Formatters.Binary;
using System.Runtime.InteropServices;
using System.Globalization;
using System.Web;
using System.Collections;

namespace Westwind.Tools
{

    /// <summary>
    /// wwUtils class which contains a set of common utility classes for 
    /// Formatting strings
    /// Reflection Helpers
    /// Object Serialization
    /// </summary>
    public partial class wwUtils
    {


        /// <summary>
        /// Retrieve a dynamic 'non-typelib' property
        /// </summary>
        /// <param name="Object">Object to make the call on</param>
        /// <param name="Property">Property to retrieve</param>
        /// <returns></returns>
        public static object GetPropertyCom(object Object, string Property)
        {
            return Object.GetType().InvokeMember(Property, wwUtils.MemberAccess | BindingFlags.GetProperty | BindingFlags.GetField, null,
                                                Object, null);
        }


        /// <summary>
        /// Returns a property or field value using a base object and sub members including . syntax.
        /// For example, you can access: this.oCustomer.oData.Company with (this,"oCustomer.oData.Company")
        /// </summary>
        /// <param name="Parent">Parent object to 'start' parsing from.</param>
        /// <param name="Property">The property to retrieve. Example: 'oBus.oData.Company'</param>
        /// <returns></returns>
        public static object GetPropertyExCom(object Parent, string Property)
        {

            Type Type = Parent.GetType();

            int lnAt = Property.IndexOf(".");
            if (lnAt < 0)
            {
                if (Property == "this" || Property == "me")
                    return Parent;

                // *** Get the member
                return Parent.GetType().InvokeMember(Property, wwUtils.MemberAccess | BindingFlags.GetProperty | BindingFlags.GetField, null,
                    Parent, null);
            }

            // *** Walk the . syntax - split into current object (Main) and further parsed objects (Subs)
            string Main = Property.Substring(0, lnAt);
            string Subs = Property.Substring(lnAt + 1);

            object Sub = Parent.GetType().InvokeMember(Main, wwUtils.MemberAccess | BindingFlags.GetProperty | BindingFlags.GetField, null,
                Parent, null);

            // *** Recurse further into the sub-properties (Subs)
            return wwUtils.GetPropertyExCom(Sub, Subs);
        }

        /// <summary>
        /// Sets the property on an object.
        /// </summary>
        /// <param name="Object">Object to set property on</param>
        /// <param name="Property">Name of the property to set</param>
        /// <param name="Value">value to set it to</param>
        public static void SetPropertyCom(object Object, string Property, object Value)
        {
            Object.GetType().InvokeMember(Property, wwUtils.MemberAccess | BindingFlags.SetProperty | BindingFlags.SetField, null, Object, new object[1] { Value });
            //GetProperty(Property,wwUtils.MemberAccess).SetValue(Object,Value,null);
        }

        /// <summary>
        /// Sets the value of a field or property via Reflection. This method alws 
        /// for using '.' syntax to specify objects multiple levels down.
        /// 
        /// wwUtils.SetPropertyEx(this,"Invoice.LineItemsCount",10)
        /// 
        /// which would be equivalent of:
        /// 
        /// this.Invoice.LineItemsCount = 10;
        /// </summary>
        /// <param name="Object Parent">
        /// Object to set the property on.
        /// </param>
        /// <param name="String Property">
        /// Property to set. Can be an object hierarchy with . syntax.
        /// </param>
        /// <param name="Object Value">
        /// Value to set the property to
        /// </param>
        public static object SetPropertyExCom(object Parent, string Property, object Value)
        {
            Type Type = Parent.GetType();

            int lnAt = Property.IndexOf(".");
            if (lnAt < 0)
            {
                // *** Set the member
                Parent.GetType().InvokeMember(Property, wwUtils.MemberAccess | BindingFlags.SetProperty | BindingFlags.SetField, null,
                    Parent, new object[1] { Value });

                return null;
            }

            // *** Walk the . syntax - split into current object (Main) and further parsed objects (Subs)
            string Main = Property.Substring(0, lnAt);
            string Subs = Property.Substring(lnAt + 1);


            object Sub = Parent.GetType().InvokeMember(Main, wwUtils.MemberAccess | BindingFlags.GetProperty | BindingFlags.GetField, null,
                Parent, null);

            return SetPropertyExCom(Sub, Subs, Value);
        }


        /// <summary>
        /// Wrapper method to call a 'dynamic' (non-typelib) method
        /// on a COM object
        /// </summary>
        /// <param name="Params"></param>
        /// 1st - Method name, 2nd - 1st parameter, 3rd - 2nd parm etc.
        /// <returns></returns>
        public static object CallMethodCom(object Object, string Method, params object[] Params)
        {
            return Object.GetType().InvokeMember(Method, wwUtils.MemberAccess | BindingFlags.InvokeMethod, null, Object, Params);
        }

    }
}





